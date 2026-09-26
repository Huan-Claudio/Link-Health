package br.edu.pucgoias.app.util;

import android.app.Activity;
import android.content.Intent;

import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.paciente.HomePacienteActivity;

/** Atalhos de navegação usados por mais de uma tela. */
public final class Navegacao {

    private Navegacao() {
    }

    /** Abre a Home do perfil e limpa a pilha (o botão voltar não retorna ao login). */
    public static void abrirHome(Activity origem, Perfil perfil) {
        Class<?> destino = HomePacienteActivity.class;
        Intent intent = new Intent(origem, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        origem.startActivity(intent);
        origem.finish();
    }
}
