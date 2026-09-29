package br.edu.pucgoias.app.ui.comum;

import android.os.Bundle;
import android.widget.EditText;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.Formatador;

/**
 * Figma: "Editar Informações Paciente" / "Editar Informações Nutricionista".
 * Se receber MockData.EXTRA_PACIENTE, edita os dados daquele paciente (aberto pela Ficha do Paciente).
 */
public class EditarInformacoesActivity extends BaseActivity {

    private EditText etNome;
    private EditText etEmail;
    private EditText etTelefone;
    private EditText etDataNascimento;
    private Paciente pacienteEditado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_informacoes);
        configurarHeader(R.string.editar_informacoes);

        etNome = findViewById(R.id.etNome);
        etEmail = findViewById(R.id.etEmail);
        etTelefone = findViewById(R.id.etTelefone);
        etDataNascimento = findViewById(R.id.etDataNascimento);
        Formatador.configurarCampoData(this, findViewById(R.id.etDataNascimento));

        if (getIntent().hasExtra(MockData.EXTRA_PACIENTE)) {
            pacienteEditado = MockData.getPaciente(getIntent().getIntExtra(MockData.EXTRA_PACIENTE, -1));
        } else if (Sessao.getPerfil() == Perfil.PACIENTE) {
            pacienteEditado = MockData.getPacienteLogado();
        }

        if (pacienteEditado != null) {
            etNome.setText(pacienteEditado.getNome());
            etEmail.setText(pacienteEditado.getEmail());
            etTelefone.setText(pacienteEditado.getTelefone());
            etDataNascimento.setText(pacienteEditado.getDataNascimento());
        } else {
            etNome.setText(Sessao.getNomeUsuario());
        }

        findViewById(R.id.btnSalvar).setOnClickListener(v -> salvar());
    }

    private void salvar() {
        String nome = etNome.getText().toString().trim();
        if (nome.isEmpty()) {
            etNome.setError(getString(R.string.campo_obrigatorio));
            return;
        }
        String email = etEmail.getText().toString().trim();
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(getString(R.string.email_invalido));
            return;
        }

        // TODO: enviar as alterações para o back-end.
        if (pacienteEditado != null) {
            pacienteEditado.setNome(nome);
            pacienteEditado.setEmail(email);
            pacienteEditado.setTelefone(etTelefone.getText().toString().trim());
            pacienteEditado.setDataNascimento(etDataNascimento.getText().toString().trim());
        }
        if (!getIntent().hasExtra(MockData.EXTRA_PACIENTE)) {
            Sessao.setNomeUsuario(nome);
        }
        toast(R.string.informacoes_salvas);
        finish();
    }
}
