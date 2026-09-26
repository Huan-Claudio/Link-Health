package br.edu.pucgoias.app.model;

/** Produto recomendado pelo nutricionista, com link externo de compra. */
public class Produto {
    private String nome;
    private String preco;
    private String link;

    public Produto(String nome, String preco, String link) {
        this.nome = nome;
        this.preco = preco;
        this.link = link;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getPreco() { return preco; }
    public void setPreco(String preco) { this.preco = preco; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
}
