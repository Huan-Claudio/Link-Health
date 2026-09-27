package br.edu.pucgoias.app.util;

import android.text.Editable;
import android.text.TextWatcher;

/** TextWatcher simplificado: permite usar lambda (etCampo.addTextChangedListener(new TextoAlterado(t -> ...))). */
public class TextoAlterado implements TextWatcher {

    public interface Acao {
        void onTexto(String texto);
    }

    private final Acao acao;

    public TextoAlterado(Acao acao) {
        this.acao = acao;
    }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        acao.onTexto(s.toString());
    }
}
