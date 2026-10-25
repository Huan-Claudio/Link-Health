package br.edu.pucgoias.app.model;

/** Produto recomendado pelo nutricionista, com link externo de compra. */
public class Produto {
    private java.util.UUID id;
    private String nome;
    private String preco;
    private String link;
    private String moeda = "BRL";

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
    public java.util.UUID getId() { return id; }
    public void setId(java.util.UUID id) { this.id = id; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda == null ? "BRL" : moeda; }

    public String getPrecoFormatado() {
        if (preco == null || preco.trim().isEmpty()) return "Preço não informado";
        try {
            java.math.BigDecimal valor = new java.math.BigDecimal(preco.replace(',', '.'));
            java.text.NumberFormat formato = java.text.NumberFormat.getCurrencyInstance(
                    java.util.Locale.forLanguageTag("pt-BR"));
            formato.setCurrency(java.util.Currency.getInstance(moeda));
            return formato.format(valor);
        } catch (IllegalArgumentException exception) {
            return moeda + " " + preco;
        }
    }
}
