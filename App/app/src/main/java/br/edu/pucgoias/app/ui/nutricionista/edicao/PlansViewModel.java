package br.edu.pucgoias.app.ui.nutricionista.edicao;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import br.edu.pucgoias.app.data.remote.ClinicalApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado da tela "Editar Plano Alimentar". Sobrevive à rotação: o que foi digitado e as
 * operações em andamento continuam no ViewModel.
 *
 * Regras do servidor que a tela respeita:
 * - um plano publicado não é alterado; ao salvar a primeira mudança, ele é copiado para um rascunho;
 * - o paciente só vê planos publicados (três refeições, cada uma com pelo menos um alimento).
 * Não há botão "Publicar": depois de cada "Salvar Alterações" (ou remoção de refeição) o plano é
 * publicado automaticamente quando já cumpre esse mínimo.
 */
public class PlansViewModel extends ViewModel {

    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    /** Cards exibidos (um por refeição). */
    public final List<RascunhoRefeicao> refeicoes = new ArrayList<>();
    /** Aumenta sempre que a lista de cards é refeita (a tela redesenha todos os cards). */
    public int versaoLista;
    public boolean loading, loaded, busy;
    @Nullable public String error;

    private String patient, followUp, draftId;
    @Nullable private JSONObject plan;
    private String[] tipos = new String[0];
    @Nullable private String mensagem;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ClinicalApi api = new ClinicalApi();
    private boolean cleared;

    private interface Resultado { void ok(JSONObject plano) throws JSONException; }

    public void start(String patientId, String[] tiposRefeicao) {
        if (patient != null) return;
        patient = patientId;
        tipos = tiposRefeicao;
        load();
    }

    // ---------- Estado lido pela tela ----------

    public boolean temPlano() { return plan != null; }
    public boolean isRascunho() { return plan != null && !plan.optBoolean("published"); }
    public boolean temAcompanhamento() { return followUp != null; }

    /** Mensagem curta (toast) que deve aparecer uma única vez. */
    @Nullable
    public String consumirMensagem() {
        String m = mensagem;
        mensagem = null;
        return m;
    }

    public boolean temAlteracoesNaoSalvas() {
        for (RascunhoRefeicao r : refeicoes) if (r.isAlterado()) return true;
        return false;
    }

    // ---------- Carregamento ----------

    public void load() {
        if (loading || busy || patient == null) return;
        loading = true;
        error = null;
        changed();
        api.request("GET", "/api/v1/me/follow-ups", null, callback(body -> {
            JSONArray links = new JSONArray(body);
            followUp = null;
            for (int i = 0; i < links.length(); i++) {
                JSONObject f = links.getJSONObject(i);
                if (patient.equals(f.optString("patientId")) && "ATIVO".equals(f.optString("status"))) {
                    followUp = f.getString("id");
                    break;
                }
            }
            if (followUp == null) {
                plan = null;
                refeicoes.clear();
                versaoLista++;
                loading = false;
                loaded = true;
                error = "É necessário um acompanhamento ativo com este paciente para montar o plano.";
                changed();
                return;
            }
            buscarPlanos(() -> {
                loading = false;
                loaded = true;
                changed();
            });
        }));
    }

    /** Escolhe o plano exibido: o rascunho em edição ou, se não houver, o plano publicado. */
    private void buscarPlanos(Runnable fim) {
        api.request("GET", "/api/v1/follow-ups/" + followUp + "/meal-plans", null, callback(body -> {
            JSONArray planos = new JSONArray(body);
            JSONObject rascunho = null;
            JSONObject ativo = null;
            for (int i = 0; i < planos.length(); i++) {
                JSONObject p = planos.getJSONObject(i);
                if (!p.optBoolean("published")) {
                    if (rascunho == null || p.optString("id").equals(draftId)) rascunho = p;
                } else if (p.optBoolean("active")) {
                    ativo = p;
                }
            }
            aplicarPlano(rascunho != null ? rascunho : ativo, null);
            fim.run();
        }));
    }

    // ---------- Ações dos cards ----------

    /** Novo card local; só vai para o servidor ao tocar em "Salvar Alterações". */
    public int adicionarRefeicao(String nome) {
        refeicoes.add(new RascunhoRefeicao(nome, "00:00"));
        return refeicoes.size() - 1;
    }

    /** Envia o card: cria/atualiza a refeição e depois apaga, cria e altera os alimentos. */
    public void salvar(RascunhoRefeicao r) {
        if (busy || loading) return;
        String problema = validar(r);
        if (problema != null) {
            mensagem = problema;
            changed();
            return;
        }
        busy = true;
        r.salvando = true;
        error = null;
        changed();
        garantirRascunho(() -> salvarCabecalho(r));
    }

    public void remover(RascunhoRefeicao r) {
        if (busy || loading) return;
        if (r.mealId == null) { // card novo, ainda não existe no servidor
            refeicoes.remove(r);
            versaoLista++;
            changed();
            return;
        }
        busy = true;
        r.salvando = true;
        error = null;
        changed();
        garantirRascunho(() -> enviar("DELETE", caminhoRefeicoes() + "/" + r.mealId, null, novo -> {
            refeicoes.remove(r);
            plan = novo;
            concluir(null, "Refeição removida.");
        }));
    }

    // ---------- Passos do salvamento ----------

    @Nullable
    private String validar(RascunhoRefeicao r) {
        if (r.nome.trim().isEmpty() || r.nome.trim().length() > 50) {
            return "Escolha o tipo da refeição.";
        }
        if (!r.horario.matches("([01][0-9]|2[0-3]):[0-5][0-9]")) return "Escolha o horário da refeição.";
        boolean algumAlimento = false;
        for (AlimentoRascunho a : r.alimentos) {
            if (a.texto.trim().length() > 150) return "Cada alimento pode ter até 150 caracteres.";
            if (!a.texto.trim().isEmpty()) algumAlimento = true;
        }
        // Uma refeição sem nenhum alimento preenchido não é salva.
        if (!algumAlimento) return "Erro, deve possuir pelo menos um campo de refeição preenchido";
        return null;
    }

    /**
     * O servidor só altera rascunhos. Se não houver plano, cria um; se o exibido estiver publicado,
     * copia para um rascunho e passa os cards para os ids da cópia (mesma ordem de refeições e alimentos).
     */
    private void garantirRascunho(Runnable depois) {
        if (isRascunho()) {
            depois.run();
        } else if (plan != null) {
            JSONObject origem = plan;
            enviar("POST", "/api/v1/meal-plans/" + origem.optString("id") + "/copy", null, copia -> {
                remapear(origem, copia);
                plan = copia;
                draftId = copia.getString("id");
                depois.run();
            });
        } else if (followUp != null) {
            enviar("POST", "/api/v1/follow-ups/" + followUp + "/meal-plans", null, novo -> {
                plan = novo;
                draftId = novo.getString("id");
                depois.run();
            });
        } else {
            falhar("É necessário um acompanhamento ativo com este paciente para montar o plano.");
        }
    }

    private void salvarCabecalho(RascunhoRefeicao r) {
        JSONObject corpo = ClinicalApi.body("name", r.nome.trim(), "time", r.horario + ":00");
        if (r.mealId == null) {
            enviar("POST", caminhoRefeicoes(), corpo, novo -> {
                JSONArray meals = novo.getJSONArray("meals");
                r.mealId = meals.getJSONObject(meals.length() - 1).getString("id");
                r.nomeSalvo = r.nome;
                r.horarioSalvo = r.horario;
                plan = novo;
                salvarAlimentos(r);
            });
        } else if (r.isCabecalhoAlterado()) {
            enviar("PUT", caminhoRefeicoes() + "/" + r.mealId, corpo, novo -> {
                r.nomeSalvo = r.nome;
                r.horarioSalvo = r.horario;
                plan = novo;
                salvarAlimentos(r);
            });
        } else {
            salvarAlimentos(r);
        }
    }

    /** Uma requisição por vez; cada resposta já traz o plano atualizado. */
    private void salvarAlimentos(RascunhoRefeicao r) {
        String itens = caminhoRefeicoes() + "/" + r.mealId + "/items";
        if (!r.alimentosRemovidos.isEmpty()) {
            String id = r.alimentosRemovidos.get(0);
            enviar("DELETE", itens + "/" + id, null, novo -> {
                r.alimentosRemovidos.remove(id);
                plan = novo;
                salvarAlimentos(r);
            });
            return;
        }
        for (AlimentoRascunho a : r.alimentos) {
            String texto = a.texto.trim();
            if (a.itemId == null && !texto.isEmpty()) {
                enviar("POST", itens, ClinicalApi.body("description", texto), novo -> {
                    a.itemId = ultimoItem(novo, r.mealId);
                    a.textoSalvo = texto;
                    plan = novo;
                    salvarAlimentos(r);
                });
                return;
            }
            if (a.itemId != null && texto.isEmpty()) { // campo apagado: remove o alimento
                enviar("DELETE", itens + "/" + a.itemId, null, novo -> {
                    a.itemId = null;
                    a.textoSalvo = "";
                    plan = novo;
                    salvarAlimentos(r);
                });
                return;
            }
            if (a.itemId != null && !texto.equals(a.textoSalvo)) {
                enviar("PUT", itens + "/" + a.itemId, ClinicalApi.body("description", texto), novo -> {
                    a.textoSalvo = texto;
                    plan = novo;
                    salvarAlimentos(r);
                });
                return;
            }
        }
        concluir(r, "Refeição Salva");
    }

    /**
     * Fim de um salvamento ou remoção. Se o rascunho já tem o mínimo exigido, publica na hora
     * para o paciente ver. Ao salvar um card o aviso é só "Refeição Salva"; ao remover uma
     * refeição, avisa o que falta para o paciente ver o plano.
     */
    private void concluir(@Nullable RascunhoRefeicao salvo, String ok) {
        if (isRascunho() && planoCompleto(plan)) {
            enviar("POST", "/api/v1/meal-plans/" + plan.optString("id") + "/activate", null, novo -> {
                draftId = null;
                terminar(novo, salvo, ok);
            });
        } else {
            terminar(plan, salvo, isRascunho() && salvo == null
                    ? ok + " O paciente verá o plano quando houver pelo menos três refeições, cada uma com um alimento."
                    : ok);
        }
    }

    private void terminar(@Nullable JSONObject novo, @Nullable RascunhoRefeicao salvo, String aviso) {
        busy = false;
        if (salvo != null) salvo.salvando = false;
        aplicarPlano(novo, salvo);
        mensagem = aviso;
        changed();
    }

    /** Mínimo do servidor para publicar: três refeições, cada uma com pelo menos um alimento. */
    private static boolean planoCompleto(@Nullable JSONObject plano) {
        JSONArray meals = plano == null ? null : plano.optJSONArray("meals");
        if (meals == null || meals.length() < 3) return false;
        for (int i = 0; i < meals.length(); i++) {
            JSONObject meal = meals.optJSONObject(i);
            JSONArray itens = meal == null ? null : meal.optJSONArray("items");
            if (itens == null || itens.length() == 0) return false;
        }
        return true;
    }

    // ---------- Auxiliares ----------

    private String caminhoRefeicoes() {
        return "/api/v1/meal-plans/" + plan.optString("id") + "/meals";
    }

    private static String ultimoItem(JSONObject plano, String mealId) throws JSONException {
        JSONArray meals = plano.getJSONArray("meals");
        for (int i = 0; i < meals.length(); i++) {
            JSONObject meal = meals.getJSONObject(i);
            if (mealId.equals(meal.optString("id"))) {
                JSONArray itens = meal.getJSONArray("items");
                return itens.getJSONObject(itens.length() - 1).getString("id");
            }
        }
        throw new JSONException("Refeição não encontrada");
    }

    /** Troca os ids do plano publicado pelos ids da cópia (a cópia mantém a mesma ordem). */
    private void remapear(JSONObject origem, JSONObject copia) {
        JSONArray de = origem.optJSONArray("meals");
        JSONArray para = copia.optJSONArray("meals");
        if (de == null || para == null) return;
        for (RascunhoRefeicao r : refeicoes) {
            if (r.mealId == null) continue;
            int i = indice(de, r.mealId);
            if (i < 0 || i >= para.length()) {
                r.mealId = null;
                continue;
            }
            JSONArray itensDe = de.optJSONObject(i).optJSONArray("items");
            JSONArray itensPara = para.optJSONObject(i).optJSONArray("items");
            r.mealId = para.optJSONObject(i).optString("id");
            for (AlimentoRascunho a : r.alimentos) {
                if (a.itemId != null) a.itemId = idCorrespondente(itensDe, itensPara, a.itemId);
            }
            for (int k = 0; k < r.alimentosRemovidos.size(); k++) {
                String novo = idCorrespondente(itensDe, itensPara, r.alimentosRemovidos.get(k));
                if (novo == null) r.alimentosRemovidos.remove(k--);
                else r.alimentosRemovidos.set(k, novo);
            }
        }
    }

    @Nullable
    private static String idCorrespondente(@Nullable JSONArray de, @Nullable JSONArray para, String id) {
        if (de == null || para == null) return null;
        int i = indice(de, id);
        return i >= 0 && i < para.length() ? para.optJSONObject(i).optString("id") : null;
    }

    private static int indice(JSONArray lista, String id) {
        for (int i = 0; i < lista.length(); i++) {
            JSONObject o = lista.optJSONObject(i);
            if (o != null && id.equals(o.optString("id"))) return i;
        }
        return -1;
    }

    /**
     * Refaz os cards a partir do plano do servidor, preservando cards com alterações ainda não
     * salvas (exceto o que acabou de ser salvo, que passa a mostrar o que ficou gravado).
     */
    private void aplicarPlano(@Nullable JSONObject novo, @Nullable RascunhoRefeicao salvo) {
        plan = novo;
        if (novo != null && !novo.optBoolean("published")) draftId = novo.optString("id");
        List<RascunhoRefeicao> anteriores = new ArrayList<>(refeicoes);
        refeicoes.clear();
        JSONArray meals = novo == null ? null : novo.optJSONArray("meals");
        for (int i = 0; meals != null && i < meals.length(); i++) {
            JSONObject meal = meals.optJSONObject(i);
            if (meal == null) continue;
            RascunhoRefeicao local = null;
            for (RascunhoRefeicao r : anteriores) {
                if (meal.optString("id").equals(r.mealId)) local = r;
            }
            if (local != null && local != salvo && local.isAlterado()) refeicoes.add(local);
            else refeicoes.add(RascunhoRefeicao.doServidor(meal, tipos));
        }
        for (RascunhoRefeicao r : anteriores) {
            if (r.mealId == null && r != salvo) refeicoes.add(r); // cards novos ainda não salvos
        }
        versaoLista++;
    }

    private void enviar(String metodo, String caminho, @Nullable JSONObject corpo, Resultado resultado) {
        api.request(metodo, caminho, corpo, callback(body -> resultado.ok(new JSONObject(body))));
    }

    private interface Corpo { void aceitar(String body) throws JSONException; }

    private ClinicalApi.Callback callback(Corpo corpo) {
        return new ClinicalApi.Callback() {
            @Override public void success(String body) {
                main.post(() -> {
                    if (cleared) return;
                    try {
                        corpo.aceitar(body);
                    } catch (JSONException | RuntimeException e) {
                        falhar("Resposta inválida do servidor. Toque aqui para atualizar.");
                    }
                });
            }

            @Override public void error(String message) {
                main.post(() -> {
                    if (!cleared) falhar(message);
                });
            }
        };
    }

    /** Para tudo e mostra o erro; o que já foi salvo fica registrado nos cards (pode tentar de novo). */
    private void falhar(String message) {
        busy = false;
        loading = false;
        for (RascunhoRefeicao r : refeicoes) r.salvando = false;
        error = message;
        changed();
    }

    private void changed() {
        if (!cleared) changes.setValue(changes.getValue() == null ? 1 : changes.getValue() + 1);
    }

    @Override
    protected void onCleared() {
        cleared = true;
        main.removeCallbacksAndMessages(null);
    }
}
