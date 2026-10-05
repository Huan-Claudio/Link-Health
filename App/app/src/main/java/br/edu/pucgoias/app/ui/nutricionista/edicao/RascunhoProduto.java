package br.edu.pucgoias.app.ui.nutricionista.edicao;

import br.edu.pucgoias.app.model.Produto;

/** Cópia editável de um produto recomendado: só vai para o paciente ao tocar em "Salvar Alterações". */
public class RascunhoProduto {

    public final Produto original;
    public String nome;
    /** Preço no formato do campo numérico (ponto decimal). */
    public String preco;
    public String link;

    public RascunhoProduto(Produto original) {
        this.original = original;
        nome = original.getNome();
        preco = original.getPreco().replace(",", ".");
        link = original.getLink();
    }

    public void aplicar() {
        original.setNome(nome.trim());
        original.setPreco(preco.trim().replace(".", ","));
        original.setLink(link.trim());
    }
}
