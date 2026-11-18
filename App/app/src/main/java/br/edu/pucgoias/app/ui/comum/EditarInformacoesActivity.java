package br.edu.pucgoias.app.ui.comum;

import android.os.Bundle;
import android.content.Intent;
import android.util.Patterns;
import androidx.lifecycle.ViewModelProvider;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityEditarInformacoesBinding;
import br.edu.pucgoias.app.ui.auth.LoginActivity;
import br.edu.pucgoias.app.util.Formatador;
import br.edu.pucgoias.app.util.TextoAlterado;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** O usuário edita somente seu perfil autenticado. */
public class EditarInformacoesActivity extends BaseActivity {
    private ActivityEditarInformacoesBinding binding;
    private ProfileViewModel model;
    private boolean filled;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        if (!Sessao.temSessaoRemota()) { finish(); return; }
        binding = ActivityEditarInformacoesBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        configurarHeader(binding.header, R.string.editar_informacoes);
        Formatador.configurarCampoData(this, binding.etDataNascimento);
        model = new ViewModelProvider(this).get(ProfileViewModel.class);
        binding.etNome.addTextChangedListener(new TextoAlterado(v -> model.name = v));
        binding.etEmail.addTextChangedListener(new TextoAlterado(v -> model.email = v));
        binding.etTelefone.addTextChangedListener(new TextoAlterado(v -> model.phone = v));
        binding.etDataNascimento.addTextChangedListener(new TextoAlterado(v -> model.birth = v));
        binding.etSenhaAtual.addTextChangedListener(new TextoAlterado(v -> model.currentPassword = v));
        binding.etNovaSenha.addTextChangedListener(new TextoAlterado(v -> model.newPassword = v));
        binding.btnSalvar.setOnClickListener(v -> { if (!model.loaded) model.load(); else save(); });
        model.changes.observe(this, ignored -> {
            if (model.loaded && !filled) {
                filled = true;
                binding.etNome.setText(model.name); binding.etEmail.setText(model.email); binding.etTelefone.setText(model.phone);
                binding.etDataNascimento.setText(model.birth); binding.etSenhaAtual.setText(model.currentPassword); binding.etNovaSenha.setText(model.newPassword);
            }
            boolean editable = model.loaded && !model.busy;
            binding.etNome.setEnabled(editable); binding.etEmail.setEnabled(editable); binding.etTelefone.setEnabled(editable);
            binding.etDataNascimento.setEnabled(editable); binding.etSenhaAtual.setEnabled(editable); binding.etNovaSenha.setEnabled(editable);
            binding.btnSalvar.setEnabled(!model.busy);
            binding.btnSalvar.setText(model.loaded ? "Salvar" : "Tentar novamente");
            binding.tvPerfilStatus.setText(model.busy ? "Aguarde a confirmação do servidor..." : model.error == null ? "Para alterar e-mail/senha, confirme a senha atual. Você entrará novamente após a troca." : model.error);
            if (model.done) {
                model.done = false;
                if (model.reauthenticate) {
                    Sessao.sair();
                    toast("Dados salvos. Entre novamente com seu e-mail e senha.");
                    startActivity(new Intent(this, LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                } else toast(R.string.informacoes_salvas);
                finish();
            }
        });
        if (!model.loaded) model.load();
    }
    private void save() {
        if (model.name.trim().isEmpty() || model.name.trim().length() > 150) { binding.etNome.setError("Informe um nome com até 150 caracteres."); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(model.email.trim()).matches()) { binding.etEmail.setError(getString(R.string.email_invalido)); return; }
        if (model.phone.trim().length() > 20) { binding.etTelefone.setError("Use até 20 caracteres."); return; }
        if (!model.newPassword.isEmpty() && (model.newPassword.length() < 8 || model.newPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)) { binding.etNovaSenha.setError("Use pelo menos 8 caracteres e até 72 bytes UTF-8."); return; }
        String date = "";
        if (!model.birth.isEmpty()) {
            try {
                LocalDate parsed = LocalDate.parse(model.birth, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
                if (!parsed.isBefore(LocalDate.now())) throw new IllegalArgumentException();
                date = parsed.toString();
            } catch (Exception e) { binding.etDataNascimento.setError("Informe uma data de nascimento anterior a hoje."); return; }
        }
        model.save(date);
    }
}
