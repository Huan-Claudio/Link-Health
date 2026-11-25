package br.edu.pucgoias.app.ui.nutricionista.edicao;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Cópia editável de uma refeição do plano (card da tela "Editar Plano Alimentar").
 * As mudanças ficam só aqui até o nutricionista tocar em "Salvar Alterações" daquele card.
 */
public class RascunhoRefeicao {

    /** Id da refeição no servidor (null para um card novo que ainda não foi salvo). */
    @Nullable public String mealId;
    public String nome;
    public String horario;
    public String nomeSalvo;
    public String horarioSalvo;
    public final List<AlimentoRascunho> alimentos = new ArrayList<>();
    /** Ids dos alimentos que estavam salvos e foram removidos com a lixeira. */
    public final List<String> alimentosRemovidos = new ArrayList<>();
    /** true enquanto este card está sendo enviado ao servidor. */
    public boolean salvando;

    /** Card novo, criado pelo botão "+ Adicionar Refeição". */
    public RascunhoRefeicao(String nome, String horario) {
        this.nome = nome;
        this.horario = horario;
        this.nomeSalvo = nome;
        this.horarioSalvo = horario;
    }

    /** Card de uma refeição que já está gravada no plano. */
    public static RascunhoRefeicao doServidor(JSONObject meal, String[] tiposRefeicao) {
        String nome = padronizarTipo(meal.optString("name"), tiposRefeicao);
        RascunhoRefeicao r = new RascunhoRefeicao(nome, horarioCurto(meal.optString("time")));
        r.mealId = meal.optString("id");
        JSONArray itens = meal.optJSONArray("items");
        if (itens != null) {
            for (int i = 0; i < itens.length(); i++) {
                JSONObject item = itens.optJSONObject(i);
                if (item != null) {
                    r.alimentos.add(new AlimentoRascunho(item.optString("id"), item.optString("description")));
                }
            }
        }
        return r;
    }

    /** true se o nome ou o horário mudaram desde o último salvamento. */
    public boolean isCabecalhoAlterado() {
        return !nome.equals(nomeSalvo) || !horario.equals(horarioSalvo);
    }

    /** true se existe qualquer alteração deste card que ainda não foi salva. */
    public boolean isAlterado() {
        if (mealId == null || isCabecalhoAlterado() || !alimentosRemovidos.isEmpty()) return true;
        for (AlimentoRascunho alimento : alimentos) if (alimento.isAlterado()) return true;
        return false;
    }

    /** Chamado pela lixeira de um alimento: se ele já estava salvo, guarda o id para apagar no servidor. */
    public void alimentoRemovido(CampoTexto campo) {
        if (campo instanceof AlimentoRascunho) {
            String id = ((AlimentoRascunho) campo).itemId;
            if (id != null) alimentosRemovidos.add(id);
        }
    }

    /** O servidor devolve "08:00:00"; a tela mostra "08:00". */
    static String horarioCurto(String horario) {
        return horario != null && horario.matches("\\d{2}:\\d{2}.*") ? horario.substring(0, 5) : "00:00";
    }

    /** Usa a grafia da lista de tipos ("Café da Manhã" -> "Café da manhã"). */
    private static String padronizarTipo(String nome, String[] tiposRefeicao) {
        for (String tipo : tiposRefeicao) {
            if (tipo.equalsIgnoreCase(nome.trim())) return tipo;
        }
        return nome.trim();
    }
}
