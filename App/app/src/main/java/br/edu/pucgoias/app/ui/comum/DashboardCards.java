package br.edu.pucgoias.app.ui.comum;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.util.Formatador;

/** Preenche os cards compartilhados entre o Dashboard do Paciente e a Ficha do Paciente. */
public final class DashboardCards {

    private DashboardCards() {
    }

    /** Card azul: objetivo + subtítulo (nutricionista ou paciente). */
    public static void preencherObjetivo(View card, String objetivo, String subtitulo) {
        ((TextView) card.findViewById(R.id.tvObjetivo)).setText(objetivo);
        ((TextView) card.findViewById(R.id.tvSubtituloObjetivo)).setText(subtitulo);
    }

    /** Card "Resumo de Hoje". */
    public static void atualizarResumo(View card, Paciente p) {
        Context c = card.getContext();
        int total = p.getPlano().size();
        int feitas = p.getRefeicoesConcluidas();
        LinearProgressIndicator progress = card.findViewById(R.id.progressRefeicoes);
        progress.setProgressCompat(Formatador.porcentagem(feitas, total), true);
        ((TextView) card.findViewById(R.id.tvRefeicoesConcluidas))
                .setText(c.getString(R.string.refeicoes_concluidas, feitas, total));
    }

    /** Card "Meta de água ingerida". */
    public static void atualizarAgua(View card, Paciente p) {
        Context c = card.getContext();
        LinearProgressIndicator progress = card.findViewById(R.id.progressAgua);
        progress.setProgressCompat(Formatador.porcentagem(p.getAguaConsumida(), p.getMetaAgua()), true);
        ((TextView) card.findViewById(R.id.tvAgua)).setText(c.getString(R.string.agua_valor,
                Formatador.litros(p.getAguaConsumida()), Formatador.litros(p.getMetaAgua())));
    }
}
