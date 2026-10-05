package br.edu.pucgoias.app.ui.paciente;

import android.content.Intent;
import android.os.Bundle;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.databinding.ActivityHomePacienteBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.comum.ConfiguracoesActivity;
import br.edu.pucgoias.app.ui.comum.DashboardCards;
import br.edu.pucgoias.app.ui.comum.EvolucaoCorporalActivity;

/** Figma: "Home/Dashboard Paciente". */
public class HomePacienteActivity extends BaseActivity {

    private ActivityHomePacienteBinding binding;
    private Paciente paciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        binding = ActivityHomePacienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        paciente = MockData.getPacienteLogado();
        DashboardCards.preencherObjetivo(binding.cardObjetivo, paciente.getObjetivo(),
                getString(R.string.nutricionista_de, MockData.NOME_NUTRICIONISTA_COMPLETO));

        // Botões de água: -500ml, -1L, +500ml, +1L
        binding.cardAgua.btnMenos500.setOnClickListener(v -> adicionarAgua(-0.5));
        binding.cardAgua.btnMenos1L.setOnClickListener(v -> adicionarAgua(-1));
        binding.cardAgua.btnMais500.setOnClickListener(v -> adicionarAgua(0.5));
        binding.cardAgua.btnMais1L.setOnClickListener(v -> adicionarAgua(1));

        configurarMenu(binding.menuPlano, R.drawable.ic_restaurant, R.string.menu_plano,
                R.string.menu_plano_desc, v -> startActivity(new Intent(this, PlanoAlimentarActivity.class)));
        configurarMenu(binding.menuConvites, R.drawable.ic_mail, R.string.menu_convites,
                R.string.menu_convites_desc, v -> startActivity(new Intent(this, ConvitesActivity.class)));
        configurarMenu(binding.menuEvolucao, R.drawable.ic_camera, R.string.menu_evolucao,
                R.string.menu_evolucao_desc, v -> startActivity(new Intent(this, EvolucaoCorporalActivity.class)));
        configurarMenu(binding.menuLista, R.drawable.ic_bag, R.string.menu_lista,
                R.string.menu_lista_desc, v -> startActivity(new Intent(this, ListaComprasActivity.class)));
        configurarMenu(binding.menuPerfil, R.drawable.ic_person, R.string.menu_perfil,
                R.string.menu_perfil_desc, v -> startActivity(new Intent(this, ConfiguracoesActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta do plano alimentar com refeições marcadas → atualiza o resumo.
        DashboardCards.atualizarResumo(binding.cardResumo, paciente);
        DashboardCards.atualizarAgua(binding.cardAgua, paciente);
    }

    private void adicionarAgua(double litros) {
        paciente.adicionarAgua(litros);
        DashboardCards.atualizarAgua(binding.cardAgua, paciente);
    }
}
