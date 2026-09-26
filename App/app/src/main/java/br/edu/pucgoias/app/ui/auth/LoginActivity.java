package br.edu.pucgoias.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.Navegacao;
import br.edu.pucgoias.app.util.PerfilToggle;

/** Figma: "Login and role selection". */
public class LoginActivity extends BaseActivity {

    private PerfilToggle toggle;
    private EditText etLogin;
    private EditText etSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView tvLabelLogin = findViewById(R.id.tvLabelLogin);
        etLogin = findViewById(R.id.etLogin);
        etSenha = findViewById(R.id.etSenha);

        // O rótulo muda conforme o perfil: paciente entra com CPF, nutricionista com CRM.
        toggle = new PerfilToggle(findViewById(R.id.togglePerfil), Perfil.PACIENTE, perfil ->
                tvLabelLogin.setText(perfil == Perfil.NUTRICIONISTA
                        ? R.string.label_email_crm : R.string.label_email_cpf));

        findViewById(R.id.btnEntrar).setOnClickListener(v -> entrar());
        findViewById(R.id.btnCriarConta).setOnClickListener(v -> {
            Intent intent = new Intent(this, CadastroActivity.class);
            intent.putExtra(CadastroActivity.EXTRA_PERFIL, toggle.getPerfil().name());
            startActivity(intent);
        });
    }

    private void entrar() {
        boolean valido = true;
        if (etLogin.getText().toString().trim().isEmpty()) {
            etLogin.setError(getString(R.string.campo_obrigatorio));
            valido = false;
        }
        if (etSenha.getText().toString().isEmpty()) {
            etSenha.setError(getString(R.string.campo_obrigatorio));
            valido = false;
        }
        if (!valido) return;
        if (toggle.getPerfil() == Perfil.NUTRICIONISTA) {
            toast(R.string.nutricionista_em_breve);
            return;
        }

        // TODO: validar usuário e senha no back-end.
        Sessao.entrar(toggle.getPerfil());
        Navegacao.abrirHome(this, toggle.getPerfil());
    }
}
