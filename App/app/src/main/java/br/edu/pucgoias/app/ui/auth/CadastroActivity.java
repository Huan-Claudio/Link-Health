package br.edu.pucgoias.app.ui.auth;

import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.widget.EditText;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.AccountApi;
import br.edu.pucgoias.app.data.remote.AccountSession;
import br.edu.pucgoias.app.databinding.ActivityCadastroBinding;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.Formatador;
import br.edu.pucgoias.app.util.Navegacao;
import br.edu.pucgoias.app.util.PerfilToggle;

/** Figma: "Cadastro Paciente" e "Cadastro Nutricionista". */
public class CadastroActivity extends BaseActivity {

    /** Extra opcional com o nome do Perfil (Perfil.name()) já selecionado no login. */
    public static final String EXTRA_PERFIL = "br.edu.pucgoias.app.EXTRA_PERFIL";

    private ActivityCadastroBinding binding;
    private PerfilToggle toggle;
    private final AccountApi accountApi = new AccountApi();

    @Override
    protected boolean exigeSessao() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCadastroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, R.string.criar_conta);

        Formatador.configurarCampoData(this, binding.etDataNascimento);

        // Nutricionista informa o CRN; paciente informa o CPF.
        toggle = new PerfilToggle(binding.togglePerfil, perfilDaIntent(), perfil -> {
            boolean nutri = perfil == Perfil.NUTRICIONISTA;
            binding.tvLabelDocumento.setText(nutri ? R.string.label_crn : R.string.label_cpf);
            binding.etDocumento.setHint(nutri ? R.string.hint_crn : R.string.hint_cpf);
            binding.etDocumento.setInputType(nutri
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                    : InputType.TYPE_CLASS_NUMBER);
            binding.etDocumento.setText("");
        });

        binding.btnFinalizar.setOnClickListener(v -> finalizar());
    }

    /** Lê o perfil enviado pelo login sem travar se o valor vier inválido. */
    private Perfil perfilDaIntent() {
        String extra = getIntent().getStringExtra(EXTRA_PERFIL);
        if (extra == null) return Perfil.PACIENTE;
        try {
            return Perfil.valueOf(extra);
        } catch (IllegalArgumentException e) {
            return Perfil.PACIENTE;
        }
    }

    private void finalizar() {
        EditText[] obrigatorios = {binding.etNome, binding.etEmail, binding.etDocumento, binding.etSenha};
        boolean valido = true;
        for (EditText campo : obrigatorios) {
            if (campo.getText().toString().trim().isEmpty()) {
                campo.setError(getString(R.string.campo_obrigatorio));
                valido = false;
            }
        }
        if (!valido) return;

        String nome = binding.etNome.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String senha = binding.etSenha.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError(getString(R.string.email_invalido));
            return;
        }
        if (senha.length() < 8) {
            binding.etSenha.setError(getString(R.string.senha_minima));
            return;
        }

        setEnviando(true);
        accountApi.register(new AccountApi.RegisterData(
                nome,
                binding.etDataNascimento.getText().toString().trim(),
                email,
                binding.etTelefone.getText().toString().trim(),
                binding.etDocumento.getText().toString().trim(),
                senha,
                toggle.getPerfil()), new AccountApi.Callback() {
            @Override
            public void onSuccess(AccountSession session) {
                runOnUiThread(() -> concluirCadastro(session));
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

    private void concluirCadastro(AccountSession session) {
        if (isFinishing() || isDestroyed()) return;
        Sessao.entrar(session);
        toast(R.string.cadastro_sucesso);
        Navegacao.abrirHome(this, session.getPerfil());
    }

    private void setEnviando(boolean enviando) {
        binding.btnFinalizar.setEnabled(!enviando);
        binding.btnFinalizar.setText(enviando ? R.string.aguarde : R.string.finalizar_cadastro);
    }
}
