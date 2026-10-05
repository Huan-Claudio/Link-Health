package br.edu.pucgoias.app.ui.comum;

import android.content.Context;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ViewCardAguaBinding;
import br.edu.pucgoias.app.databinding.ViewCardResumoBinding;
import br.edu.pucgoias.app.databinding.ViewHeaderCardBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.util.Formatador;

/** Preenche os cards compartilhados entre o Dashboard do Paciente e a Ficha do Paciente. */
public final class DashboardCards {

    private DashboardCards() {
    }

    /** Card azul: objetivo + subtítulo (nutricionista ou paciente). */
    public static void preencherObjetivo(ViewHeaderCardBinding card, String objetivo, String subtitulo) {
        card.tvObjetivo.setText(objetivo);
        card.tvSubtituloObjetivo.setText(subtitulo);
    }

    /** Card "Resumo de Hoje". */
    public static void atualizarResumo(ViewCardResumoBinding card, Paciente p) {
        Context c = card.getRoot().getContext();
        int total = p.getPlano().size();
        int feitas = p.getRefeicoesConcluidas();
        card.progressRefeicoes.setProgressCompat(Formatador.porcentagem(feitas, total), true);
        card.tvRefeicoesConcluidas.setText(c.getString(R.string.refeicoes_concluidas, feitas, total));
    }

    /** Card "Meta de água ingerida". */
    public static void atualizarAgua(ViewCardAguaBinding card, Paciente p) {
        Context c = card.getRoot().getContext();
        card.progressAgua.setProgressCompat(Formatador.porcentagem(p.getAguaConsumida(), p.getMetaAgua()), true);
        card.tvAgua.setText(c.getString(R.string.agua_valor,
                Formatador.litros(p.getAguaConsumida()), Formatador.litros(p.getMetaAgua())));
    }
}
