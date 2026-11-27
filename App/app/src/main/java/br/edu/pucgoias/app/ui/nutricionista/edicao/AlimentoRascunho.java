package br.edu.pucgoias.app.ui.nutricionista.edicao;

import androidx.annotation.Nullable;

/**
 * Alimento de uma refeição em edição.
 * itemId é o id do item no servidor (null enquanto o alimento ainda não foi salvo)
 * e textoSalvo é o texto que está gravado no servidor, usado para saber se houve alteração.
 */
public class AlimentoRascunho extends CampoTexto {

    @Nullable public String itemId;
    public String textoSalvo;

    public AlimentoRascunho(@Nullable String itemId, String texto) {
        super(texto);
        this.itemId = itemId;
        this.textoSalvo = itemId == null ? "" : this.texto;
    }

    /** true se o texto digitado ainda não foi enviado ao servidor. */
    public boolean isAlterado() {
        String atual = texto.trim();
        return itemId == null ? !atual.isEmpty() : !atual.equals(textoSalvo);
    }
}
