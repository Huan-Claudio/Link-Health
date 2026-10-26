package br.edu.pucgoias.app.ui.paciente;

import android.content.Intent;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityHomePacienteBinding;
import br.edu.pucgoias.app.ui.comum.ConfiguracoesActivity;
import br.edu.pucgoias.app.ui.comum.EvolucaoCorporalActivity;
import br.edu.pucgoias.app.ui.comum.RecordsViewModel;
import br.edu.pucgoias.app.ui.comum.RecordFormatting;
import br.edu.pucgoias.app.ui.nutricionista.RegistroDiarioActivity;
import org.json.JSONObject;

/** Dashboard alimentado pelos registros da conta autenticada. */
public class HomePacienteActivity extends BaseActivity {
    private ActivityHomePacienteBinding binding;
    private RecordsViewModel vm;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        binding = ActivityHomePacienteBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        binding.cardAgua.btnMenos500.setOnClickListener(v -> vm.water(-500));
        binding.cardAgua.btnMenos1L.setOnClickListener(v -> vm.water(-1000));
        binding.cardAgua.btnMais500.setOnClickListener(v -> vm.water(500));
        binding.cardAgua.btnMais1L.setOnClickListener(v -> vm.water(1000));
        configurarMenu(binding.menuPlano, R.drawable.ic_restaurant, R.string.menu_plano,
                R.string.menu_plano_desc, v -> startActivity(new Intent(this, PlanoAlimentarActivity.class)));
        configurarMenu(binding.menuConvites, R.drawable.ic_mail, R.string.menu_convites,
                R.string.menu_convites_desc, v -> startActivity(new Intent(this, ConvitesActivity.class)));
        configurarMenu(binding.menuEvolucao, R.drawable.ic_camera, R.string.menu_evolucao,
                R.string.menu_evolucao_desc, v -> startActivity(new Intent(this, EvolucaoCorporalActivity.class)));
        configurarMenu(binding.menuLista, R.drawable.ic_bag, R.string.menu_lista,
                R.string.menu_lista_desc, v -> startActivity(new Intent(this, ListaComprasActivity.class)));
        configurarMenu(binding.menuHistorico, R.drawable.ic_assignment, R.string.records_history_title,
                R.string.records_history_description, v -> startActivity(new Intent(this, RegistroDiarioActivity.class)));
        configurarMenu(binding.menuPerfil, R.drawable.ic_person, R.string.menu_perfil,
                R.string.menu_perfil_desc, v -> startActivity(new Intent(this, ConfiguracoesActivity.class)));
        binding.cardResumo.getRoot().setOnClickListener(v -> startActivity(new Intent(this, RegistroDiarioActivity.class)));
        vm = new ViewModelProvider(this).get(RecordsViewModel.class);
        vm.changes.observe(this, ignored -> render());
        binding.recordsStatus.setOnClickListener(v -> vm.reload());
        if (Sessao.temSessaoRemota()) vm.configure(Sessao.getUsuarioId(), Sessao.getAccessToken(), true);
        else vm.report("Entre novamente com uma conta cadastrada para carregar seus registros.");
    }
    @Override protected void onResume() { super.onResume(); if (vm != null) vm.reload(); }
    private void render() {
        boolean ready = Sessao.temSessaoRemota() && vm.ready() && vm.summary != null;
        binding.cardAgua.btnMenos500.setEnabled(ready); binding.cardAgua.btnMenos1L.setEnabled(ready);
        binding.cardAgua.btnMais500.setEnabled(ready); binding.cardAgua.btnMais1L.setEnabled(ready);
        binding.recordsStatus.setText(!vm.error.isEmpty() ? vm.error + " Toque para tentar novamente."
                : vm.busy ? "Salvando consumo…" : vm.loading > 0 ? "Carregando seus registros…" : "Atualizar registros");
        JSONObject s = vm.summary;
        if (s == null) {
            binding.cardObjetivo.tvObjetivo.setText("Seu acompanhamento");
            binding.cardObjetivo.tvSubtituloObjetivo.setText(Sessao.getNomeUsuario());
            binding.cardAgua.tvAgua.setText("Consumo ainda não carregado");
            binding.cardResumo.tvRefeicoesConcluidas.setText("Resumo ainda não carregado"); return;
        }
        String objective = RecordFormatting.value(s, "objective"), nutritionist = RecordFormatting.value(s, "nutritionistName");
        binding.cardObjetivo.tvObjetivo.setText(objective.isEmpty() ? "Objetivo ainda não definido" : objective);
        binding.cardObjetivo.tvSubtituloObjetivo.setText(nutritionist.isEmpty() ? "Sem acompanhamento ativo" : "Nutricionista: " + nutritionist);
        int water = s.optInt("waterMilliliters"), goal = s.optInt("waterGoalMilliliters");
        binding.cardAgua.tvAgua.setText(water + " ml consumidos" + (goal > 0 ? " de " + goal + " ml" : " · meta não definida"));
        binding.cardAgua.progressAgua.setProgressCompat(goal > 0 ? Math.min(100, (int) (100L * water / goal)) : 0, true);
        int done = s.optInt("completedMeals"), planned = s.optInt("plannedMeals");
        binding.cardResumo.tvRefeicoesConcluidas.setText(done + " refeições registradas hoje" + (planned > 0 ? " · " + planned + " planejadas" : " · sem plano ativo"));
        binding.cardResumo.progressRefeicoes.setVisibility(android.view.View.GONE);
    }
}
