package br.edu.pucgoias.app.ui.nutricionista.edicao;

import androidx.annotation.Nullable;

import br.edu.pucgoias.app.model.ItemCompra;

/** Linha da lista de compras em edição: o item original (null se for novo) e o texto digitado. */
public class LinhaCompra extends CampoTexto {

    @Nullable public final ItemCompra original;

    public LinhaCompra(@Nullable ItemCompra original, String texto) {
        super(texto);
        this.original = original;
    }
}
