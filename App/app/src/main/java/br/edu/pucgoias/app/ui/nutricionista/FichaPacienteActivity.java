package br.edu.pucgoias.app.ui.nutricionista;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import br.edu.pucgoias.app.databinding.ActivityFichaPacienteBinding;
import br.edu.pucgoias.app.ui.comum.EvolucaoCorporalActivity;
import br.edu.pucgoias.app.ui.paciente.PlanoAlimentarActivity;
import br.edu.pucgoias.app.util.Formatador;
import br.edu.pucgoias.app.util.PatientNavigation;

import org.json.JSONObject;

import java.util.UUID;

/** Resumo e atalhos dos registros do paciente vinculado, sem dados de demonstração. */
public class FichaPacienteActivity extends BaseActivity {
    private ActivityFichaPacienteBinding binding;
    private UUID patient;
    private String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        patient = PatientNavigation.patientId(getIntent());
        name = PatientNavigation.name(getIntent());
        if (patient == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        binding = ActivityFichaPacienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnVoltar.setOnClickListener(v -> finish());
        binding.cardAgua.layoutBotoesAgua.setVisibility(View.GONE);
        binding.cardAgua.btnEditarMeta.setVisibility(View.VISIBLE);
        binding.cardAgua.btnEditarMeta.setOnClickListener(v -> editarAcompanhamento());
        binding.cardObjetivo.getRoot().setOnClickListener(v -> editarAcompanhamento());
        configurarMenu(
                binding.menuEditarPlano,
                R.drawable.ic_restaurant,
                R.string.menu_editar_plano,
                R.string.menu_editar_plano_desc,
                v -> abrir(PlanoAlimentarActivity.class));
        binding.menuEditarPlano.tvTitulo.setText("Refeições realizadas");
        binding.menuEditarPlano.tvDescricao.setText(
                "Consultar as refeições e os registros do paciente");
        configurarMenu(
                binding.menuEditarProdutos,
                R.drawable.ic_bag,
                R.string.menu_editar_produtos,
                R.string.menu_editar_produtos_desc,
                v -> abrir(EditarProdutosActivity.class));
        configurarMenu(
                binding.menuEvolucao,
                R.drawable.ic_camera,
                R.string.menu_evolucao,
                R.string.menu_evolucao_nutri_desc,
                v -> abrir(EvolucaoCorporalActivity.class));
        configurarMenu(
                binding.menuEditarLista,
                R.drawable.ic_cart,
                R.string.menu_editar_lista,
                R.string.menu_editar_lista_desc,
                v -> abrir(EditarListaComprasActivity.class));
        configurarMenu(
                binding.menuRegistros,
                R.drawable.ic_assignment,
                R.string.menu_registros,
                R.string.menu_registros_desc,
                v -> abrir(RegistroDiarioActivity.class));
        binding.cardResumo.getRoot().setOnClickListener(v -> carregar());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) carregar();
    }

    private void carregar() {
        binding.cardObjetivo.tvObjetivo.setText("Carregando registros…");
        binding.cardObjetivo.tvSubtituloObjetivo.setText(name + " · toque para editar o objetivo");
        binding.cardResumo.tvRefeicoesConcluidas.setText("Carregando…");
        binding.cardAgua.tvAgua.setText("Carregando…");
        new RecordsApi()
                .summary(
                        patient,
                        Sessao.getAccessToken(),
                        br.edu.pucgoias.app.ui.comum.RecordFormatting.todayDate(),
                        new RecordsApi.Callback<JSONObject>() {
                            public void onSuccess(JSONObject data) {
                                runOnUiThread(
                                        () -> {
                                            if (isFinishing() || isDestroyed()) return;
                                            binding.cardObjetivo.tvObjetivo.setText(
                                                    data.isNull("objective")
                                                            ? "Objetivo não informado"
                                                            : data.optString("objective"));
                                            int done = data.optInt("completedMeals"),
                                                    total = data.optInt("plannedMeals");
                                            binding.cardResumo.tvRefeicoesConcluidas.setText(
                                                    total > 0
                                                            ? done
                                                                    + " refeição(ões) registrada(s)"
                                                                    + " · "
                                                                    + total
                                                                    + " planejada(s)"
                                                            : done
                                                                    + " refeição(ões) registrada(s)"
                                                                    + " hoje");
                                            binding.cardResumo.progressRefeicoes.setProgressCompat(
                                                    Formatador.porcentagem(done, total), false);
                                            int water = data.optInt("waterMilliliters"),
                                                    goal = data.optInt("waterGoalMilliliters");
                                            binding.cardAgua.tvAgua.setText(
                                                    goal > 0
                                                            ? water + " ml / " + goal + " ml"
                                                            : water + " ml · meta não informada");
                                            binding.cardAgua.progressAgua.setProgressCompat(
                                                    Formatador.porcentagem(water, goal), false);
                                        });
                            }

                            public void onError(String message) {
                                runOnUiThread(
                                        () -> {
                                            if (isFinishing() || isDestroyed()) return;
                                            binding.cardObjetivo.tvObjetivo.setText(
                                                    "Registros indisponíveis");
                                            binding.cardResumo.tvRefeicoesConcluidas.setText(
                                                    message + "\nToque para tentar novamente.");
                                            binding.cardAgua.tvAgua.setText("—");
                                            toast(message);
                                        });
                            }
                        });
    }

    private void abrir(Class<? extends Activity> screen) {
        startActivity(PatientNavigation.intent(this, screen, patient, name));
    }

    private void editarAcompanhamento() {
        registrarDialog(
                br.edu.pucgoias.app.ui.comum.CareSettingsDialog.show(
                        this, patient, this::carregar));
    }
}
