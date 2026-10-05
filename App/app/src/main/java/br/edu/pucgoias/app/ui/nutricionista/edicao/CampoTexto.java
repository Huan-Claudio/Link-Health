package br.edu.pucgoias.app.ui.nutricionista.edicao;

/** Texto editável de uma linha (alimento do plano ou item da lista de compras) enquanto não é salvo. */
public class CampoTexto {

    public String texto;

    public CampoTexto(String texto) {
        this.texto = texto == null ? "" : texto;
    }

    public boolean isVazio() {
        return texto.trim().isEmpty();
    }
}
