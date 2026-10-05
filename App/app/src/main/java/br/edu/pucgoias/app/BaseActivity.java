package br.edu.pucgoias.app;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.DialogInputBinding;
import br.edu.pucgoias.app.databinding.ItemMenuCardBinding;
import br.edu.pucgoias.app.databinding.ViewHeaderBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.auth.LoginActivity;
import br.edu.pucgoias.app.util.Navegacao;

/**
 * Activity base do Link Health.
 * - Ativa o modo edge-to-edge e aplica o espaçamento das barras do sistema/teclado.
 * - Protege as telas quando a sessão se perde (o Android pode encerrar o processo em segundo plano).
 * - Oferece utilitários comuns (cabeçalho, cards de menu, diálogos de texto e toasts) usando View Binding.
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Callback usado pelos diálogos de entrada de texto. */
    public interface TextoCallback {
        void onTexto(String texto);
    }

    private boolean sessaoExpirada;
    /** Diálogo aberto no momento; é fechado em onDestroy para não vazar a janela ao girar a tela. */
    @Nullable private Dialog dialogAberto;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);

        // Se o Android encerrou o processo com o app em segundo plano, a sessão em memória foi perdida.
        // Em vez de abrir a tela sem usuário (e arriscar um crash), volta para o login.
        if (exigeSessao() && !Sessao.isLogado()) {
            sessaoExpirada = true;
            toast(R.string.sessao_expirada);
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    /** Telas que não precisam de usuário logado (login e cadastro) retornam false. */
    protected boolean exigeSessao() {
        return true;
    }

    /** As telas chamam isto logo após super.onCreate(): se true, não devem montar a interface. */
    protected boolean sessaoExpirada() {
        return sessaoExpirada;
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        aplicarInsets(view);
    }

    @Override
    protected void onDestroy() {
        if (dialogAberto != null && dialogAberto.isShowing()) dialogAberto.dismiss();
        dialogAberto = null;
        super.onDestroy();
    }

    /** Guarda o diálogo exibido para fechá-lo junto com a tela. */
    protected void registrarDialog(Dialog dialog) {
        dialogAberto = dialog;
    }

    /** Soma o tamanho das barras do sistema (e do teclado) ao padding original da tela. */
    private static void aplicarInsets(View root) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets barras = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(left + barras.left, top + barras.top,
                    right + barras.right, bottom + barras.bottom);
            return insets;
        });
    }

    /** Configura o cabeçalho padrão (layout view_header.xml). */
    protected void configurarHeader(ViewHeaderBinding header, String titulo) {
        header.tvTituloHeader.setText(titulo);
        header.btnVoltar.setOnClickListener(v -> finish());
    }

    protected void configurarHeader(ViewHeaderBinding header, @StringRes int titulo) {
        configurarHeader(header, getString(titulo));
    }

    /** Preenche um card de menu incluído com &lt;include layout="@layout/item_menu_card"&gt;. */
    protected void configurarMenu(ItemMenuCardBinding menu, @DrawableRes int icone, @StringRes int titulo,
                                  @StringRes int descricao, View.OnClickListener acao) {
        menu.ivIcone.setImageResource(icone);
        menu.tvTitulo.setText(titulo);
        menu.tvDescricao.setText(descricao);
        menu.getRoot().setOnClickListener(acao);
    }

    /** Abre um diálogo simples com um campo de texto. */
    protected void pedirTexto(String titulo, String hint, int inputType, @Nullable String valorInicial,
                              TextoCallback callback) {
        DialogInputBinding dialogBinding = DialogInputBinding.inflate(getLayoutInflater());
        dialogBinding.etDialog.setHint(hint);
        dialogBinding.etDialog.setInputType(inputType == 0 ? InputType.TYPE_CLASS_TEXT : inputType);
        if (valorInicial != null) {
            dialogBinding.etDialog.setText(valorInicial);
            dialogBinding.etDialog.setSelection(valorInicial.length());
        }
        registrarDialog(new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.salvar, (d, w) -> {
                    String texto = dialogBinding.etDialog.getText().toString().trim();
                    if (!texto.isEmpty()) callback.onTexto(texto);
                })
                .show());
    }

    /**
     * Lê o paciente enviado pela Intent explícita (identificado pelo CPF, que não muda
     * quando a lista é reordenada). Retorna null se o extra faltar ou não corresponder a ninguém.
     */
    @Nullable
    protected Paciente pacienteDaIntent() {
        String cpf = getIntent().getStringExtra(Navegacao.EXTRA_PACIENTE_CPF);
        return cpf == null ? null : MockData.buscarPorCpf(cpf);
    }

    /** Fecha a tela com um aviso quando o paciente recebido é inválido (em vez de travar o app). */
    protected void fecharPacienteNaoEncontrado() {
        toast(R.string.paciente_nao_encontrado);
        finish();
    }

    protected void toast(@StringRes int mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    protected void toast(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }
}
