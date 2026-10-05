package br.edu.pucgoias.app.ui.comum;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityConfiguracoesBinding;
import br.edu.pucgoias.app.databinding.ItemConfigOpcaoBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.auth.LoginActivity;

/** Figma: "Perfil Paciente" e "Perfil Nutricionista" (tela Configurações). */
public class ConfiguracoesActivity extends BaseActivity {

    private ActivityConfiguracoesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        binding = ActivityConfiguracoesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configurarHeader(binding.header, R.string.configuracoes);

        boolean nutri = Sessao.getPerfil() == Perfil.NUTRICIONISTA;
        if (nutri) {
            binding.tvPacientesAtivos.setVisibility(View.VISIBLE);
            binding.tvPacientesAtivos.setText(getString(R.string.pacientes_ativos, MockData.getPacientes().size()));
        }

        configurarOpcao(binding.opcaoDados, R.drawable.ic_phone,
                nutri ? R.string.alterar_dados_pessoais : R.string.dados_contato,
                v -> startActivity(new Intent(this, EditarInformacoesActivity.class)));
        configurarOpcao(binding.opcaoEndereco, R.drawable.ic_location, R.string.endereco,
                v -> toast(R.string.em_breve));
        configurarOpcao(binding.opcaoNotificacoes, R.drawable.ic_notifications, R.string.notificacoes,
                v -> binding.opcaoNotificacoes.switchOpcao.toggle());
        binding.opcaoNotificacoes.switchOpcao.setVisibility(View.VISIBLE);
        if (savedInstanceState == null) binding.opcaoNotificacoes.switchOpcao.setChecked(true);

        binding.btnEditarFoto.setOnClickListener(v -> toast(R.string.em_breve));
        binding.btnSair.setOnClickListener(v -> sair());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Atualiza nome/iniciais caso tenham sido alterados em "Editar Informações".
        String nome = Sessao.getNomeUsuario();
        boolean nutri = Sessao.getPerfil() == Perfil.NUTRICIONISTA;
        binding.tvAvatar.setText(nutri ? "DR" : Paciente.iniciais(nome));
        binding.tvOlaNome.setText(getString(R.string.ola_nome, nome));
    }

    private void configurarOpcao(ItemConfigOpcaoBinding opcao, @DrawableRes int icone, @StringRes int titulo,
                                 @Nullable View.OnClickListener acao) {
        opcao.ivIcone.setImageResource(icone);
        opcao.tvTitulo.setText(titulo);
        opcao.getRoot().setOnClickListener(acao);
    }

    private void sair() {
        Sessao.sair();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
