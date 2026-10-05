package br.edu.pucgoias.app.ui.nutricionista;

import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ActivityFichaPacienteBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.comum.DashboardCards;
import br.edu.pucgoias.app.ui.comum.EvolucaoCorporalActivity;
import br.edu.pucgoias.app.util.Formatador;
import br.edu.pucgoias.app.util.Navegacao;

/** Figma: "Ficha do Paciente (Tela do nutricionista)". */
public class FichaPacienteActivity extends BaseActivity {

    private ActivityFichaPacienteBinding binding;
    private Paciente paciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        paciente = pacienteDaIntent();
        if (paciente == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        binding = ActivityFichaPacienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnVoltar.setOnClickListener(v -> finish());

        // Nutricionista não registra água: ele só ajusta a meta.
        binding.cardAgua.layoutBotoesAgua.setVisibility(View.GONE);
        binding.cardAgua.btnEditarMeta.setVisibility(View.VISIBLE);
        binding.cardAgua.btnEditarMeta.setOnClickListener(v -> editarMeta());

        configurarMenu(binding.menuEditarPlano, R.drawable.ic_restaurant, R.string.menu_editar_plano,
                R.string.menu_editar_plano_desc, v -> abrir(EditarPlanoActivity.class));
        configurarMenu(binding.menuEditarProdutos, R.drawable.ic_bag, R.string.menu_editar_produtos,
                R.string.menu_editar_produtos_desc, v -> abrir(EditarProdutosActivity.class));
        configurarMenu(binding.menuEvolucao, R.drawable.ic_camera, R.string.menu_evolucao,
                R.string.menu_evolucao_nutri_desc, v -> abrir(EvolucaoCorporalActivity.class));
        configurarMenu(binding.menuEditarLista, R.drawable.ic_cart, R.string.menu_editar_lista,
                R.string.menu_editar_lista_desc, v -> abrir(EditarListaComprasActivity.class));
        configurarMenu(binding.menuRegistros, R.drawable.ic_assignment, R.string.menu_registros,
                R.string.menu_registros_desc, v -> abrir(RegistroDiarioActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        DashboardCards.preencherObjetivo(binding.cardObjetivo, paciente.getObjetivo(),
                getString(R.string.paciente_de, paciente.getNome()));
        DashboardCards.atualizarResumo(binding.cardResumo, paciente);
        DashboardCards.atualizarAgua(binding.cardAgua, paciente);
    }

    private void editarMeta() {
        pedirTexto(getString(R.string.nova_meta_agua), "5",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                Formatador.numero(paciente.getMetaAgua()), texto -> {
                    double meta = Formatador.lerNumero(texto);
                    if (meta <= 0) return;
                    paciente.setMetaAgua(meta);
                    DashboardCards.atualizarAgua(binding.cardAgua, paciente);
                });
    }

    /** Abre uma tela do nutricionista com uma Intent explícita que leva o CPF do paciente. */
    private void abrir(Class<? extends Activity> tela) {
        startActivity(Navegacao.comPaciente(this, tela, paciente));
    }
}
