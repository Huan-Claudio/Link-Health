package br.edu.pucgoias.app.ui.paciente;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.comum.DashboardCards;

/** Figma: "Home/Dashboard Paciente". */
public class HomePacienteActivity extends BaseActivity {

    private Paciente paciente;
    private View cardResumo;
    private View cardAgua;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_paciente);

        paciente = MockData.getPacienteLogado();
        cardResumo = findViewById(R.id.cardResumo);
        cardAgua = findViewById(R.id.cardAgua);

        DashboardCards.preencherObjetivo(findViewById(R.id.cardObjetivo), paciente.getObjetivo(),
                getString(R.string.nutricionista_de, MockData.NOME_NUTRICIONISTA_COMPLETO));

        // Botões de água: -500ml, -1L, +500ml, +1L
        cardAgua.findViewById(R.id.btnMenos500).setOnClickListener(v -> adicionarAgua(-0.5));
        cardAgua.findViewById(R.id.btnMenos1L).setOnClickListener(v -> adicionarAgua(-1));
        cardAgua.findViewById(R.id.btnMais500).setOnClickListener(v -> adicionarAgua(0.5));
        cardAgua.findViewById(R.id.btnMais1L).setOnClickListener(v -> adicionarAgua(1));

        configurarMenu(R.id.menuPlano, R.drawable.ic_restaurant, R.string.menu_plano,
                R.string.menu_plano_desc, v -> startActivity(new Intent(this, PlanoAlimentarActivity.class)));
        configurarMenu(R.id.menuConvites, R.drawable.ic_mail, R.string.menu_convites,
                R.string.menu_convites_desc, v -> startActivity(new Intent(this, ConvitesActivity.class)));
        configurarMenu(R.id.menuEvolucao, R.drawable.ic_camera, R.string.menu_evolucao,
                R.string.menu_evolucao_desc, v -> toast(R.string.tela_em_breve));
        configurarMenu(R.id.menuLista, R.drawable.ic_bag, R.string.menu_lista,
                R.string.menu_lista_desc, v -> startActivity(new Intent(this, ListaComprasActivity.class)));
        configurarMenu(R.id.menuPerfil, R.drawable.ic_person, R.string.menu_perfil,
                R.string.menu_perfil_desc, v -> toast(R.string.tela_em_breve));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Volta do plano alimentar com refeições marcadas → atualiza o resumo.
        DashboardCards.atualizarResumo(cardResumo, paciente);
        DashboardCards.atualizarAgua(cardAgua, paciente);
    }

    private void adicionarAgua(double litros) {
        paciente.adicionarAgua(litros);
        DashboardCards.atualizarAgua(cardAgua, paciente);
    }


}
