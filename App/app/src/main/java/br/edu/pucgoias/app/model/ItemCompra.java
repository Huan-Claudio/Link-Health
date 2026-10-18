package br.edu.pucgoias.app.model;

import java.util.UUID;

/** Item da lista de compras do paciente. */
public class ItemCompra {
    private UUID id;
    private String descricao;
    private boolean comprado;

    public ItemCompra(String descricao) {
        this.descricao = descricao;
    }

    public ItemCompra(UUID id, String descricao, boolean comprado) {
        this.id = id;
        this.descricao = descricao;
        this.comprado = comprado;
    }

    public UUID getId() { return id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isComprado() { return comprado; }
    public void setComprado(boolean comprado) { this.comprado = comprado; }
}
