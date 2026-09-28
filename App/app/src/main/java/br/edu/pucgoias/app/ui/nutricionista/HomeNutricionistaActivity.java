package br.edu.pucgoias.app.ui.nutricionista;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.util.TextoAlterado;

/** Figma: "Home Nutricionista" – lista de pacientes com busca. */
public class HomeNutricionistaActivity extends BaseActivity {

    private LinearLayout container;
    private TextView tvVazio;
    private EditText etBusca;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_nutricionista);

        container = findViewById(R.id.container);
        tvVazio = findViewById(R.id.tvVazio);
        etBusca = findViewById(R.id.etBusca);

        ((TextView) findViewById(R.id.tvAvatar)).setText("DR.");
        // TODO: abrir Configurações quando a tela for adicionada.
        findViewById(R.id.layoutPerfil).setOnClickListener(v -> toast(R.string.tela_em_breve));
        findViewById(R.id.btnNovoPaciente).setOnClickListener(v ->
                startActivity(new Intent(this, BuscarPacienteActivity.class)));

        etBusca.addTextChangedListener(new TextoAlterado(texto -> montarLista()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        ((TextView) findViewById(R.id.tvOla)).setText(getString(R.string.ola_nome, Sessao.getNomeUsuario()));
        ((TextView) findViewById(R.id.tvPacientesAtivos))
                .setText(getString(R.string.pacientes_ativos, MockData.getPacientes().size()));
        montarLista();
    }

    private void montarLista() {
        container.removeAllViews();
        String filtro = normalizar(etBusca.getText().toString());
        List<Paciente> pacientes = MockData.getPacientes();
        LayoutInflater inflater = LayoutInflater.from(this);
        int exibidos = 0;

        for (int i = 0; i < pacientes.size(); i++) {
            Paciente p = pacientes.get(i);
            if (!filtro.isEmpty() && !normalizar(p.getNome()).contains(filtro)) continue;

            View item = inflater.inflate(R.layout.item_paciente, container, false);
            ((TextView) item.findViewById(R.id.tvAvatar)).setText(p.getIniciais());
            ((TextView) item.findViewById(R.id.tvNome)).setText(p.getNome());
            ((TextView) item.findViewById(R.id.tvObjetivo)).setText(p.getObjetivo());
            final int indice = i;
            item.setOnClickListener(v -> {
                Intent intent = new Intent(this, FichaPacienteActivity.class);
                intent.putExtra(MockData.EXTRA_PACIENTE, indice);
                startActivity(intent);
            });
            container.addView(item);
            exibidos++;
        }
        tvVazio.setVisibility(exibidos == 0 ? View.VISIBLE : View.GONE);
    }

    /** Remove acentos e deixa minúsculo para a busca ("José" encontra "jose"). */
    static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }
}
