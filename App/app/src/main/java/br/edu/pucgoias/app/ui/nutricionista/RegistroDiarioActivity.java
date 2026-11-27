package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.Instant;
import java.time.LocalDate;
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
import br.edu.pucgoias.app.ui.adapter.RegistroDiaAdapter;
import br.edu.pucgoias.app.ui.comum.RecordFormatting;
import br.edu.pucgoias.app.ui.comum.RecordsViewModel;
import br.edu.pucgoias.app.ui.nutricionista.edicao.PlanoRefeicoesViewModel;
import br.edu.pucgoias.app.util.PatientNavigation;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Figma: "Registro diário do paciente".
 * Um card por dia (RecyclerView): data, meta de água com a barra de progresso, as refeições do
 * plano (verde = feita, vermelho = não feita, com os alimentos de cada uma embaixo) e as
 * anotações do dia.
 * As refeições vêm só do plano alimentar, o mesmo exibido na tela de plano de quem está vendo:
 * paciente = "Meu Plano Alimentar" (plano publicado); nutricionista = "Planos alimentares"
 * (rascunho em edição, senão o publicado — PlanoRefeicoesViewModel).
 * Usada pelo paciente (Home) e pelo nutricionista (Ficha do paciente); a tela só consulta os
 * registros, não edita.
 */
public class RegistroDiarioActivity extends BaseActivity {

    private RecordsViewModel vm;
    private PlanoRefeicoesViewModel plano;
    private String patientId;
    private boolean paciente;
    private PlanoStatusAdapter status;
    private RegistroDiaAdapter adapter;
    private final List<RegistroDiaAdapter.Dia> dias = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        UUID patient = PatientNavigation.patientId(getIntent());
        if (patient == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        ActivityListaSimplesBinding binding = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        paciente = Sessao.getPerfil() == Perfil.PACIENTE;
        configurarHeader(binding.header, paciente
                ? R.string.meu_registro_diario
                : R.string.registro_diario);

        patientId = patient.toString();
        vm = new ViewModelProvider(this).get(RecordsViewModel.class);
        plano = new ViewModelProvider(this).get(PlanoRefeicoesViewModel.class);
        // Aviso no topo (carregando, erro ou lista vazia) e os cards dos dias logo abaixo.
        status = new PlanoStatusAdapter(this::recarregar);
        adapter = new RegistroDiaAdapter(dias);
        binding.lista.setLayoutManager(new LinearLayoutManager(this));
        binding.lista.setAdapter(new ConcatAdapter(status, adapter));

        vm.changes.observe(this, ignorado -> render());
        plano.changes.observe(this, ignorado -> render());
        // O resumo de hoje traz a meta de água atual; o histórico e as refeições registradas vêm
        // dos registros. As refeições esperadas vêm do plano: para o paciente, as mesmas do
        // "Meu Plano Alimentar" (planned-meals); para o nutricionista, as de "Planos alimentares".
        if (paciente) {
            vm.configure(patient, Sessao.getAccessToken(), true,
                    RecordsApi.HISTORY, RecordsApi.MEALS, RecordsApi.PLAN);
        } else {
            vm.configure(patient, Sessao.getAccessToken(), true, RecordsApi.HISTORY, RecordsApi.MEALS);
            plano.carregar(patientId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null) recarregar();
    }

    private void recarregar() {
        vm.reload();
        if (!paciente) plano.carregar(patientId);
    }

    private void render() {
        if (vm.loading > 0 || plano.carregando) {
            if (dias.isEmpty()) status.mostrar(getString(R.string.registros_carregando), "");
            return;
        }
        montarDias();
        adapter.notifyDataSetChanged();
        String erro = !vm.error.isEmpty() ? vm.error : paciente ? null : plano.erro;
        if (erro != null) {
            status.mostrar(getString(R.string.plano_status_erro),
                    erro + "\n" + getString(R.string.toque_tentar_novamente));
        } else if (dias.isEmpty()) {
            status.mostrar(getString(R.string.registros_vazio_titulo),
                    getString(R.string.registros_vazio_msg));
        } else {
            status.ocultar();
        }
    }

    /**
     * Junta o histórico por dia com as refeições do plano. Só aparecem as refeições do plano; um
     * registro conta como a refeição se tiver o id dela ou o mesmo nome (registros de versões
     * anteriores do plano e registros repetidos não viram refeições extras no card).
     */
    private void montarDias() {
        dias.clear();
        JSONObject resumo = vm.summary;
        Integer meta = resumo == null || resumo.isNull("waterGoalMilliliters")
                ? null : resumo.optInt("waterGoalMilliliters");
        JSONArray historico = vm.items(RecordsApi.HISTORY);
        JSONArray registros = vm.items(RecordsApi.MEALS);
        JSONArray refeicoesPlano = paciente ? vm.items(RecordsApi.PLAN) : plano.refeicoes;

        for (int i = 0; i < historico.length(); i++) {
            JSONObject dia = historico.optJSONObject(i);
            if (dia == null) continue;
            LocalDate data;
            try {
                data = LocalDate.parse(dia.optString("date"));
            } catch (RuntimeException e) {
                continue;
            }

            // Registros deste dia: refeições e a anotação do dia (que não é refeição).
            List<JSONObject> doDia = new ArrayList<>();
            List<JSONObject> anotacoesDoDia = new ArrayList<>();
            for (int k = 0; k < registros.length(); k++) {
                JSONObject r = registros.optJSONObject(k);
                if (r == null || !data.equals(dataDe(r.optString("consumedAt")))) continue;
                if (RecordFormatting.isAnotacao(r)) anotacoesDoDia.add(r);
                else doDia.add(r);
            }
            // Cada refeição do plano: feita se houver um registro dela no dia.
            List<RegistroDiaAdapter.Refeicao> refeicoes = new ArrayList<>();
            for (int p = 0; p < refeicoesPlano.length(); p++) {
                JSONObject planejada = refeicoesPlano.optJSONObject(p);
                if (planejada == null) continue;
                String id = planejada.optString("id");
                String nome = planejada.optString("name").trim();
                boolean feita = false;
                for (JSONObject r : doDia) {
                    if (id.equals(r.optString("mealPlanItemId"))
                            || nome.equalsIgnoreCase(r.optString("mealName").trim())) {
                        feita = true;
                        break;
                    }
                }
                refeicoes.add(new RegistroDiaAdapter.Refeicao(nome, feita, alimentosDe(planejada)));
            }

            // Anotações do dia: a anotação escrita no Meu Plano Alimentar e, depois, as
            // observações dos registros de refeição.
            StringBuilder anotacoes = new StringBuilder();
            for (JSONObject r : anotacoesDoDia) {
                String nota = RecordFormatting.value(r, "notes").trim();
                if (nota.isEmpty()) continue;
                if (anotacoes.length() > 0) anotacoes.append('\n');
                anotacoes.append(nota);
            }
            for (JSONObject r : doDia) {
                String nota = RecordFormatting.value(r, "notes").trim();
                if (nota.isEmpty()) continue;
                if (anotacoes.length() > 0) anotacoes.append('\n');
                anotacoes.append(r.optString("mealName")).append(": ").append(nota);
            }

            dias.add(new RegistroDiaAdapter.Dia(RecordFormatting.date(dia.optString("date")),
                    dia.optLong("waterMilliliters"), meta, refeicoes, anotacoes.toString()));
        }
    }

    /**
     * Alimentos da refeição do plano. planned-meals manda "foods" (textos); meal-plans manda
     * "items" ({description}).
     */
    private static List<String> alimentosDe(JSONObject refeicao) {
        List<String> alimentos = new ArrayList<>();
        JSONArray foods = refeicao.optJSONArray("foods");
        if (foods != null) {
            for (int i = 0; i < foods.length(); i++) alimentos.add(foods.optString(i));
            return alimentos;
        }
        JSONArray items = refeicao.optJSONArray("items");
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null) alimentos.add(item.optString("description"));
            }
        }
        return alimentos;
    }

    /** Dia (no fuso do app) em que o registro foi feito; null se a data vier inválida. */
    private static LocalDate dataDe(String instante) {
        try {
            return Instant.parse(instante).atZone(RecordFormatting.ZONE).toLocalDate();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
