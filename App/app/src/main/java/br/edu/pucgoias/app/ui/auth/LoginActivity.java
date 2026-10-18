package br.edu.pucgoias.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.AccountApi;
import br.edu.pucgoias.app.data.remote.AccountSession;
import br.edu.pucgoias.app.databinding.ActivityLoginBinding;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.Navegacao;
import br.edu.pucgoias.app.util.PerfilToggle;

/** Figma: "Login and role selection". */
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private PerfilToggle toggle;
    private final AccountApi accountApi = new AccountApi();

    @Override
    protected boolean exigeSessao() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // A API usa e-mail para os dois perfis; o perfil real é confirmado pela resposta do servidor.
        toggle = new PerfilToggle(binding.togglePerfil, Perfil.PACIENTE,
                perfil -> binding.tvLabelLogin.setText(R.string.label_email));

        binding.btnEntrar.setOnClickListener(v -> entrar());
        binding.btnCriarConta.setOnClickListener(v -> {
            Intent intent = new Intent(this, CadastroActivity.class);
            intent.putExtra(CadastroActivity.EXTRA_PERFIL, toggle.getPerfil().name());
            startActivity(intent);
        });
    }

    private void entrar() {
        boolean valido = true;
        String email = binding.etLogin.getText().toString().trim();
        String senha = binding.etSenha.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etLogin.setError(getString(R.string.email_invalido));
            valido = false;
        }
        if (senha.isEmpty()) {
            binding.etSenha.setError(getString(R.string.campo_obrigatorio));
            valido = false;
        }
        if (!valido) return;

        setEnviando(true);
        accountApi.login(email, senha, new AccountApi.Callback() {
            @Override
            public void onSuccess(AccountSession session) {
                runOnUiThread(() -> concluirLogin(session));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setEnviando(false);
                    toast(message);
                });
            }
        });
    }

    private void concluirLogin(AccountSession session) {
        if (isFinishing() || isDestroyed()) return;
        Sessao.entrar(session);
        Navegacao.abrirHome(this, session.getPerfil());
    }

    private void setEnviando(boolean enviando) {
        binding.btnEntrar.setEnabled(!enviando);
        binding.btnCriarConta.setEnabled(!enviando);
        binding.btnEntrar.setText(enviando ? R.string.aguarde : R.string.entrar_no_app);
    }
}
