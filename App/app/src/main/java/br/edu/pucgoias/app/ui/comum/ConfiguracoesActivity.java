package br.edu.pucgoias.app.ui.comum;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.materialswitch.MaterialSwitch;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.auth.LoginActivity;

/** Figma: "Perfil Paciente" e "Perfil Nutricionista" (tela Configurações). */
public class ConfiguracoesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);
        configurarHeader(R.string.configuracoes);

        boolean nutri = Sessao.getPerfil() == Perfil.NUTRICIONISTA;

        TextView tvPacientesAtivos = findViewById(R.id.tvPacientesAtivos);
        if (nutri) {
            tvPacientesAtivos.setVisibility(View.VISIBLE);
            tvPacientesAtivos.setText(getString(R.string.pacientes_ativos, MockData.getPacientes().size()));
        }

        configurarOpcao(R.id.opcaoDados, R.drawable.ic_phone,
                nutri ? R.string.alterar_dados_pessoais : R.string.dados_contato,
                v -> startActivity(new Intent(this, EditarInformacoesActivity.class)));
        configurarOpcao(R.id.opcaoEndereco, R.drawable.ic_location, R.string.endereco,
                v -> toast(R.string.em_breve));
        configurarOpcao(R.id.opcaoNotificacoes, R.drawable.ic_notifications, R.string.notificacoes, null);

        View notificacoes = findViewById(R.id.opcaoNotificacoes);
        MaterialSwitch sw = notificacoes.findViewById(R.id.switchOpcao);
        sw.setVisibility(View.VISIBLE);
        sw.setChecked(true);
        notificacoes.setOnClickListener(v -> sw.toggle());

        findViewById(R.id.btnEditarFoto).setOnClickListener(v -> toast(R.string.em_breve));
        findViewById(R.id.btnSair).setOnClickListener(v -> sair());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Atualiza nome/iniciais caso tenham sido alterados em "Editar Informações".
        String nome = Sessao.getNomeUsuario();
        boolean nutri = Sessao.getPerfil() == Perfil.NUTRICIONISTA;
        ((TextView) findViewById(R.id.tvAvatar)).setText(nutri ? "DR" : Paciente.iniciais(nome));
        ((TextView) findViewById(R.id.tvOlaNome)).setText(getString(R.string.ola_nome, nome));
    }

    private void configurarOpcao(int includeId, int icone, int titulo, View.OnClickListener acao) {
        View opcao = findViewById(includeId);
        ((ImageView) opcao.findViewById(R.id.ivIcone)).setImageResource(icone);
        ((TextView) opcao.findViewById(R.id.tvTitulo)).setText(titulo);
        if (acao != null) opcao.setOnClickListener(acao);
    }

    private void sair() {
        Sessao.sair();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
