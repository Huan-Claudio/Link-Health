package br.edu.pucgoias.app.model;

/** Item da lista de compras do paciente. */
public class ItemCompra {
    private String descricao;
    private boolean comprado;

    public ItemCompra(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isComprado() { return comprado; }
    public void setComprado(boolean comprado) { this.comprado = comprado; }
}
