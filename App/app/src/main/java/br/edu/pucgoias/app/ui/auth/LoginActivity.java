package br.edu.pucgoias.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityLoginBinding;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.Navegacao;
import br.edu.pucgoias.app.util.PerfilToggle;

/** Figma: "Login and role selection". */
public class LoginActivity extends BaseActivity {

    private ActivityLoginBinding binding;
    private PerfilToggle toggle;

    @Override
    protected boolean exigeSessao() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // O rótulo muda conforme o perfil: paciente entra com CPF, nutricionista com CRN.
        toggle = new PerfilToggle(binding.togglePerfil, Perfil.PACIENTE, perfil ->
                binding.tvLabelLogin.setText(perfil == Perfil.NUTRICIONISTA
                        ? R.string.label_email_crn : R.string.label_email_cpf));

        binding.btnEntrar.setOnClickListener(v -> entrar());
        binding.btnCriarConta.setOnClickListener(v -> {
            Intent intent = new Intent(this, CadastroActivity.class);
            intent.putExtra(CadastroActivity.EXTRA_PERFIL, toggle.getPerfil().name());
            startActivity(intent);
        });
    }

    private void entrar() {
        boolean valido = true;
        if (binding.etLogin.getText().toString().trim().isEmpty()) {
            binding.etLogin.setError(getString(R.string.campo_obrigatorio));
            valido = false;
        }
        if (binding.etSenha.getText().toString().isEmpty()) {
            binding.etSenha.setError(getString(R.string.campo_obrigatorio));
            valido = false;
        }
        if (!valido) return;

        // TODO: validar usuário e senha no back-end.
        Sessao.entrar(toggle.getPerfil());
        Navegacao.abrirHome(this, toggle.getPerfil());
    }
}
