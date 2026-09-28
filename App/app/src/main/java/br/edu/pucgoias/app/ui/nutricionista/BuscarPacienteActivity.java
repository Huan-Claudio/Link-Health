package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.util.TextoAlterado;

/** Figma: "Dietitian dashboard" (nutricionista) – busca paciente por CPF/e-mail e envia convite. */
public class BuscarPacienteActivity extends BaseActivity {

    private LinearLayout container;
    private TextView tvVazio;
    private EditText etBusca;
    /** Pacientes que já receberam convite nesta tela (o botão continua desativado ao refazer a busca). */
    private final Set<Paciente> convidados = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buscar_paciente);

        TextView tvAvatar = findViewById(R.id.tvAvatar);
        tvAvatar.setBackgroundResource(R.drawable.bg_circle_gray);
        tvAvatar.setText("");
        ((TextView) findViewById(R.id.tvOla)).setText(getString(R.string.ola_nome, Sessao.getNomeUsuario()));
        TextView tvSubtitulo = findViewById(R.id.tvSubtitulo);
        tvSubtitulo.setVisibility(View.VISIBLE);
        tvSubtitulo.setText(getString(R.string.pacientes_ativos, MockData.getPacientes().size()));
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        container = findViewById(R.id.container);
        tvVazio = findViewById(R.id.tvVazio);
        etBusca = findViewById(R.id.etBusca);
        etBusca.addTextChangedListener(new TextoAlterado(texto -> buscar()));
        buscar();
    }

    /** Lista os pacientes cujo CPF ou e-mail começa com o texto digitado. */
    private void buscar() {
        container.removeAllViews();
        String busca = etBusca.getText().toString().trim();
        if (busca.isEmpty()) {
            mostrarMensagem(getString(R.string.buscar_paciente_instrucao));
            return;
        }

        // TODO: refazer a busca no back-end (consultar os pacientes por CPF/e-mail no banco).
        //       Por enquanto a busca é feita nos dados de exemplo do MockData.
        List<Paciente> encontrados = MockData.buscarPacientesPorCpfOuEmail(busca);
        if (encontrados.isEmpty()) {
            mostrarMensagem(getString(R.string.nenhum_paciente));
            return;
        }
        tvVazio.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (Paciente p : encontrados) {
            View item = inflater.inflate(R.layout.item_convite, container, false);
            TextView tvAvatar = item.findViewById(R.id.tvAvatar);
            tvAvatar.setBackgroundResource(R.drawable.bg_circle_primary);
            tvAvatar.setText(p.getIniciais());
            TextView tvNome = item.findViewById(R.id.tvNome);
            tvNome.setText(p.getNome());
            View btnAdicionar = item.findViewById(R.id.btnAdicionar);

            if (MockData.getPacientes().contains(p)) {
                // Já é paciente deste nutricionista: não precisa de convite.
                tvNome.setText(getString(R.string.paciente_ja_vinculado, p.getNome()));
                desativar(btnAdicionar);
            } else if (convidados.contains(p)) {
                desativar(btnAdicionar);
            } else {
                btnAdicionar.setOnClickListener(v -> {
                    // TODO: enviar o convite pelo back-end.
                    convidados.add(p);
                    toast(getString(R.string.convite_enviado, p.getNome()));
                    desativar(v);
                });
            }
            container.addView(item);
        }
    }

    private void mostrarMensagem(String mensagem) {
        tvVazio.setText(mensagem);
        tvVazio.setVisibility(View.VISIBLE);
    }

    private static void desativar(View botao) {
        botao.setEnabled(false);
        botao.setAlpha(0.4f);
    }
}
