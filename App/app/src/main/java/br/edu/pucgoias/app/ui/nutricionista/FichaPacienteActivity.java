package br.edu.pucgoias.app.ui.nutricionista;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.comum.DashboardCards;
import br.edu.pucgoias.app.util.Formatador;

/** Figma: "Ficha do Paciente (Tela do nutricionista)". */
public class FichaPacienteActivity extends BaseActivity {

    private int indicePaciente;
    private Paciente paciente;
    private View cardResumo;
    private View cardAgua;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ficha_paciente);

        indicePaciente = getIntent().getIntExtra(MockData.EXTRA_PACIENTE, 0);
        paciente = MockData.getPaciente(indicePaciente);
        cardResumo = findViewById(R.id.cardResumo);
        cardAgua = findViewById(R.id.cardAgua);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        // TODO: abrir Editar Informações quando a tela for adicionada.
        findViewById(R.id.btnDadosPaciente).setOnClickListener(v -> toast(R.string.tela_em_breve));

        // Nutricionista não registra água: ele só ajusta a meta.
        cardAgua.findViewById(R.id.layoutBotoesAgua).setVisibility(View.GONE);
        View btnEditarMeta = cardAgua.findViewById(R.id.btnEditarMeta);
        btnEditarMeta.setVisibility(View.VISIBLE);
        btnEditarMeta.setOnClickListener(v -> editarMeta());

        // TODO: trocar os avisos pelas telas quando forem adicionadas.
        configurarMenu(R.id.menuEditarPlano, R.drawable.ic_restaurant, R.string.menu_editar_plano,
                R.string.menu_editar_plano_desc, v -> abrir(EditarPlanoActivity.class));
        configurarMenu(R.id.menuEditarProdutos, R.drawable.ic_bag, R.string.menu_editar_produtos,
                R.string.menu_editar_produtos_desc, v -> abrir(EditarProdutosActivity.class));
        configurarMenu(R.id.menuEvolucao, R.drawable.ic_camera, R.string.menu_evolucao,
                R.string.menu_evolucao_nutri_desc, v -> toast(R.string.tela_em_breve));
        configurarMenu(R.id.menuEditarLista, R.drawable.ic_cart, R.string.menu_editar_lista,
                R.string.menu_editar_lista_desc, v -> toast(R.string.tela_em_breve));
        configurarMenu(R.id.menuRegistros, R.drawable.ic_assignment, R.string.menu_registros,
                R.string.menu_registros_desc, v -> toast(R.string.tela_em_breve));
    }

    @Override
    protected void onResume() {
        super.onResume();
        DashboardCards.preencherObjetivo(findViewById(R.id.cardObjetivo), paciente.getObjetivo(),
                getString(R.string.paciente_de, paciente.getNome()));
        DashboardCards.atualizarResumo(cardResumo, paciente);
        DashboardCards.atualizarAgua(cardAgua, paciente);
    }

    private void editarMeta() {
        pedirTexto(getString(R.string.nova_meta_agua), "5",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                Formatador.numero(paciente.getMetaAgua()), texto -> {
                    double meta = Formatador.lerNumero(texto);
                    if (meta <= 0) return;
                    paciente.setMetaAgua(meta);
                    DashboardCards.atualizarAgua(cardAgua, paciente);
                });
    }

    /** Abre uma tela do nutricionista passando qual paciente está sendo editado. */
    private void abrir(Class<?> tela) {
        Intent intent = new Intent(this, tela);
        intent.putExtra(MockData.EXTRA_PACIENTE, indicePaciente);
        startActivity(intent);
    }
}
