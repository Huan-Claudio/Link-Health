package br.edu.pucgoias.app.ui.auth;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.util.Formatador;
import br.edu.pucgoias.app.util.Navegacao;
import br.edu.pucgoias.app.util.PerfilToggle;

/** Figma: "Cadastro Paciente" e "Cadastro Nutricionista". */
public class CadastroActivity extends BaseActivity {

    public static final String EXTRA_PERFIL = "extra_perfil";

    private PerfilToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);
        configurarHeader(R.string.criar_conta);

        TextView tvLabelDocumento = findViewById(R.id.tvLabelDocumento);
        EditText etDocumento = findViewById(R.id.etDocumento);
        Formatador.configurarCampoData(this, findViewById(R.id.etDataNascimento));

        String extra = getIntent().getStringExtra(EXTRA_PERFIL);
        Perfil inicial = extra != null ? Perfil.valueOf(extra) : Perfil.PACIENTE;

        // Nutricionista informa o CRM; paciente informa o CPF.
        toggle = new PerfilToggle(findViewById(R.id.togglePerfil), inicial, perfil -> {
            boolean nutri = perfil == Perfil.NUTRICIONISTA;
            tvLabelDocumento.setText(nutri ? R.string.label_crm : R.string.label_cpf);
            etDocumento.setHint(nutri ? R.string.hint_crm : R.string.hint_cpf);
            etDocumento.setInputType(nutri
                    ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                    : InputType.TYPE_CLASS_NUMBER);
            etDocumento.setText("");
        });

        findViewById(R.id.btnFinalizar).setOnClickListener(v -> finalizar());
    }

    private void finalizar() {
        int[] obrigatorios = {R.id.etNome, R.id.etEmail, R.id.etDocumento, R.id.etSenha};
        boolean valido = true;
        for (int id : obrigatorios) {
            EditText campo = findViewById(id);
            if (campo.getText().toString().trim().isEmpty()) {
                campo.setError(getString(R.string.campo_obrigatorio));
                valido = false;
            }
        }
        if (!valido) return;
        if (toggle.getPerfil() == Perfil.NUTRICIONISTA) {
            toast(R.string.nutricionista_em_breve);
            return;
        }

        // TODO: enviar o cadastro para o back-end.
        toast(R.string.cadastro_sucesso);
        Sessao.entrar(toggle.getPerfil());
        String nome = ((EditText) findViewById(R.id.etNome)).getText().toString().trim();
        Sessao.setNomeUsuario(nome);
        Navegacao.abrirHome(this, toggle.getPerfil());
    }
}
