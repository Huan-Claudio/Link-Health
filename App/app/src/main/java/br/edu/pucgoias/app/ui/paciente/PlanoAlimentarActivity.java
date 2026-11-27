package br.edu.pucgoias.app.ui.paciente;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.adapter.PlanoStatusAdapter;
import br.edu.pucgoias.app.ui.adapter.RefeicaoPacienteAdapter;
import br.edu.pucgoias.app.ui.comum.RecordFormatting;
import br.edu.pucgoias.app.ui.comum.RecordsViewModel;
import br.edu.pucgoias.app.util.PatientNavigation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Figma: "Meu Plano Alimentar" (layout activity_lista_simples.xml + RecyclerView).
 * Um card por refeição do plano publicado (horário, nome e alimentos); tocar no card marca ou
 * desmarca a refeição como feita hoje. O botão do rodapé "Adicionar anotação" grava a anotação
 * do dia, que aparece em "Anotações do dia" no Registro diário.
 */
public class PlanoAlimentarActivity extends BaseActivity {

    private ActivityListaSimplesBinding binding;
    private RecordsViewModel vm;
    private PlanoStatusAdapter status;
    private RefeicaoPacienteAdapter adapter;
    private boolean own;
    /** Registros de hoje que ainda faltam apagar para desmarcar uma refeição (apagados um por vez). */
    private final ArrayDeque<String> remocoesPendentes = new ArrayDeque<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        UUID patient = PatientNavigation.patientId(getIntent());
        if (patient == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        own = Sessao.getPerfil() == Perfil.PACIENTE;
        binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, R.string.meu_plano_alimentar);

        vm = new ViewModelProvider(this).get(RecordsViewModel.class);
        status = new PlanoStatusAdapter(() -> vm.reload());
        adapter = new RefeicaoPacienteAdapter(this::alternar);
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(new ConcatAdapter(status, adapter));

        // Rodapé: "Adicionar anotação" (só o próprio paciente escreve a anotação do dia).
        binding.btnRodape.setText(R.string.adicionar_anotacao);
        binding.btnRodape.setVisibility(own ? View.VISIBLE : View.GONE);
        binding.btnRodape.setOnClickListener(v -> abrirAnotacao());

        vm.changes.observe(this, ignorado -> render());
        vm.configure(patient, Sessao.getAccessToken(), false, RecordsApi.PLAN, RecordsApi.MEALS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null) vm.reload();
    }

    private void render() {
        JSONArray plano = vm.items(RecordsApi.PLAN);
        List<RefeicaoPacienteAdapter.Item> itens = new ArrayList<>();
        for (int i = 0; i < plano.length(); i++) {
            JSONObject meal = plano.optJSONObject(i);
            if (meal == null) continue;
            String id = meal.optString("id");
            StringBuilder alimentos = new StringBuilder();
            JSONArray foods = meal.optJSONArray("foods");
            for (int j = 0; foods != null && j < foods.length(); j++) {
                if (alimentos.length() > 0) alimentos.append('\n');
                alimentos.append("• ").append(foods.optString(j));
            }
            String horario = meal.optString("time", "");
            if (horario.length() >= 5) horario = horario.substring(0, 5);
            String nome = meal.optString("name");
            itens.add(new RefeicaoPacienteAdapter.Item(id, horario + " - " + nome,
                    alimentos.toString(), !registrosDeHoje(id, nome).isEmpty()));
        }
        adapter.atualizar(itens, own && vm.ready());
        binding.btnRodape.setEnabled(vm.ready());

        if (vm.loading > 0 && itens.isEmpty()) {
            status.mostrar(getString(R.string.plano_carregando), "");
        } else if (!vm.error.isEmpty()) {
            status.mostrar(getString(R.string.plano_status_erro),
                    vm.error + "\n" + getString(R.string.toque_tentar_novamente));
        } else if (vm.loading == 0 && itens.isEmpty()) {
            status.mostrar(getString(R.string.plano_paciente_vazio),
                    getString(R.string.plano_paciente_vazio_msg));
        } else {
            status.ocultar();
        }

        // Desmarcar: continua apagando os registros que faltam (o aviso aparece só no último).
        if (!remocoesPendentes.isEmpty()) {
            if (!vm.error.isEmpty()) {
                remocoesPendentes.clear();
            } else if (vm.ready()) {
                vm.message = "";
                vm.delete(RecordsApi.MEALS, remocoesPendentes.poll());
                return;
            }
        }

        if (!vm.message.isEmpty()) {
            toast(vm.message);
            vm.message = "";
        }
    }

    /**
     * Toque no card: marca (cria o registro de hoje) ou desmarca (apaga todos os registros de hoje
     * que contam como esta refeição, inclusive os de versões anteriores do plano, para o histórico
     * também mostrar a refeição como não feita).
     */
    private void alternar(RefeicaoPacienteAdapter.Item item) {
        if (!own || !vm.ready() || !remocoesPendentes.isEmpty()) return;
        String nome = item.titulo.contains(" - ")
                ? item.titulo.substring(item.titulo.indexOf(" - ") + 3)
                : item.titulo;
        List<JSONObject> registros = registrosDeHoje(item.id, nome);
        if (!registros.isEmpty()) {
            for (JSONObject r : registros) remocoesPendentes.add(r.optString("id"));
            vm.delete(RecordsApi.MEALS, remocoesPendentes.poll());
            return;
        }
        try {
            vm.save(RecordsApi.MEALS, null,
                    new JSONObject().put("mealPlanItemId", item.id).put("mealName", nome));
        } catch (JSONException e) {
            vm.report("Não foi possível preparar o registro.");
        }
    }

    /** Diálogo da anotação do dia: uma anotação por dia (se já existir, é editada). */
    private void abrirAnotacao() {
        if (!vm.ready()) return;
        JSONObject existente = anotacaoDeHoje();
        String atual = existente == null ? null : RecordFormatting.value(existente, "notes");
        pedirTexto(getString(R.string.anotacao_do_dia_titulo), getString(R.string.anotacao_hint),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                atual, texto -> salvarAnotacao(existente, texto));
    }

    private void salvarAnotacao(JSONObject existente, String texto) {
        if (texto.length() > 500) {
            toast(R.string.anotacao_muito_longa);
            return;
        }
        try {
            if (existente != null) {
                vm.save(RecordsApi.MEALS, existente.optString("id"),
                        new JSONObject().put("notes", texto));
            } else {
                // Um minuto antes de agora: o servidor recusa horários no futuro (relógios diferentes).
                vm.save(RecordsApi.MEALS, null, new JSONObject()
                        .put("mealName", RecordFormatting.ANOTACAO_DO_DIA)
                        .put("consumedAt", Instant.now().minusSeconds(60).toString())
                        .put("notes", texto));
            }
        } catch (JSONException e) {
            vm.report("Não foi possível preparar a anotação.");
        }
    }

    /**
     * Registros de hoje que contam como esta refeição: mesmo id ou mesmo nome (a mesma regra do
     * "Registro diário"). Vazio se a refeição ainda não foi feita hoje.
     */
    private List<JSONObject> registrosDeHoje(String mealPlanItemId, String nome) {
        List<JSONObject> encontrados = new ArrayList<>();
        JSONArray registros = vm.items(RecordsApi.MEALS);
        for (int i = 0; i < registros.length(); i++) {
            JSONObject r = registros.optJSONObject(i);
            if (r == null || RecordFormatting.isAnotacao(r)
                    || !RecordFormatting.today(r.optString("consumedAt"))) continue;
            if (mealPlanItemId.equals(r.optString("mealPlanItemId"))
                    || nome.trim().equalsIgnoreCase(r.optString("mealName").trim())) {
                encontrados.add(r);
            }
        }
        return encontrados;
    }

    /** Anotação do dia já gravada hoje (null se não houver). */
    private JSONObject anotacaoDeHoje() {
        JSONArray registros = vm.items(RecordsApi.MEALS);
        for (int i = 0; i < registros.length(); i++) {
            JSONObject r = registros.optJSONObject(i);
            if (r != null && RecordFormatting.isAnotacao(r)
                    && RecordFormatting.today(r.optString("consumedAt"))) return r;
        }
        return null;
    }
}
