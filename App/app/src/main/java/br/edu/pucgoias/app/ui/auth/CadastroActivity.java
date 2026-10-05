package br.edu.pucgoias.app.ui.auth;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityCadastroBinding;
import br.edu.pucgoias.app.model.Paciente;
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

        // TODO: enviar o cadastro para o back-end.
        toast(R.string.cadastro_sucesso);
        Sessao.entrar(toggle.getPerfil());
        String nome = binding.etNome.getText().toString().trim();
        Sessao.setNomeUsuario(nome);
        if (toggle.getPerfil() == Perfil.PACIENTE) {
            Paciente paciente = MockData.getPacienteLogado();
            paciente.setNome(nome);
            paciente.setEmail(binding.etEmail.getText().toString().trim());
            paciente.setTelefone(binding.etTelefone.getText().toString().trim());
            paciente.setDataNascimento(binding.etDataNascimento.getText().toString().trim());
        }
        Navegacao.abrirHome(this, toggle.getPerfil());
    }
}
