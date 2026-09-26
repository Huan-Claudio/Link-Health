package br.edu.pucgoias.app;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

/**
 * Activity base do Link Health.
 * - Ativa o modo edge-to-edge e aplica o espaçamento das barras do sistema/teclado.
 * - Oferece utilitários comuns (cabeçalho, cards de menu, diálogos de texto e toasts).
 */
public abstract class BaseActivity extends AppCompatActivity {

    /** Callback usado pelos diálogos de entrada de texto. */
    public interface TextoCallback {
        void onTexto(String texto);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(Color.TRANSPARENT),
                SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        aplicarInsets();
    }

    /** Soma o tamanho das barras do sistema (e do teclado) ao padding original da tela. */
    private void aplicarInsets() {
        ViewGroup content = findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) return;
        View root = content.getChildAt(0);
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
    protected void configurarHeader(String titulo) {
        TextView tvTitulo = findViewById(R.id.tvTituloHeader);
        if (tvTitulo != null) tvTitulo.setText(titulo);
        View voltar = findViewById(R.id.btnVoltar);
        if (voltar != null) voltar.setOnClickListener(v -> finish());
    }

    protected void configurarHeader(@StringRes int titulo) {
        configurarHeader(getString(titulo));
    }

    /** Preenche um card de menu incluído com &lt;include layout="@layout/item_menu_card"&gt;. */
    protected void configurarMenu(int includeId, @DrawableRes int icone, @StringRes int titulo,
                                  @StringRes int descricao, View.OnClickListener acao) {
        View card = findViewById(includeId);
        ((ImageView) card.findViewById(R.id.ivIcone)).setImageResource(icone);
        ((TextView) card.findViewById(R.id.tvTitulo)).setText(titulo);
        ((TextView) card.findViewById(R.id.tvDescricao)).setText(descricao);
        card.setOnClickListener(acao);
    }

    /** Abre um diálogo simples com um campo de texto. */
    protected void pedirTexto(String titulo, String hint, int inputType, @Nullable String valorInicial,
                              TextoCallback callback) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_input, null, false);
        EditText et = view.findViewById(R.id.etDialog);
        et.setHint(hint);
        et.setInputType(inputType == 0 ? InputType.TYPE_CLASS_TEXT : inputType);
        if (valorInicial != null) {
            et.setText(valorInicial);
            et.setSelection(valorInicial.length());
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(titulo)
                .setView(view)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.salvar, (d, w) -> {
                    String texto = et.getText().toString().trim();
                    if (!texto.isEmpty()) callback.onTexto(texto);
                })
                .show();
    }

    protected void toast(@StringRes int mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    protected void toast(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }
}
