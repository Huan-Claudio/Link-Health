package br.edu.pucgoias.app.model;

import android.graphics.Bitmap;

import androidx.annotation.Nullable;

/** Registro de evolução corporal: data, peso e (opcionalmente) a foto tirada. */
public class FotoEvolucao {
    private final String data;
    private final double peso;
    @Nullable private final Bitmap foto;

    public FotoEvolucao(String data, double peso, @Nullable Bitmap foto) {
        this.data = data;
        this.peso = peso;
        this.foto = foto;
    }

    public String getData() { return data; }
    public double getPeso() { return peso; }
    @Nullable public Bitmap getFoto() { return foto; }
}
