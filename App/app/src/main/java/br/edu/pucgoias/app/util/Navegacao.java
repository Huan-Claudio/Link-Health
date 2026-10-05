package br.edu.pucgoias.app.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.nutricionista.HomeNutricionistaActivity;
import br.edu.pucgoias.app.ui.paciente.HomePacienteActivity;

/** Intents explícitas usadas por mais de uma tela. */
public final class Navegacao {

    /** Extra com o CPF do paciente que a próxima tela deve abrir. */
    public static final String EXTRA_PACIENTE_CPF = "br.edu.pucgoias.app.EXTRA_PACIENTE_CPF";

    private Navegacao() {
    }

    /** Abre a Home do perfil e limpa a pilha (o botão voltar não retorna ao login). */
    public static void abrirHome(Activity origem, Perfil perfil) {
        Class<? extends Activity> destino = perfil == Perfil.NUTRICIONISTA
                ? HomeNutricionistaActivity.class
                : HomePacienteActivity.class;
        Intent intent = new Intent(origem, destino);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        origem.startActivity(intent);
        origem.finish();
    }

    /** Intent explícita para uma tela que trabalha sobre um paciente (o CPF identifica o paciente). */
    public static Intent comPaciente(Context origem, Class<? extends Activity> destino, Paciente paciente) {
        Intent intent = new Intent(origem, destino);
        intent.putExtra(EXTRA_PACIENTE_CPF, paciente.getCpf());
        return intent;
    }
}
