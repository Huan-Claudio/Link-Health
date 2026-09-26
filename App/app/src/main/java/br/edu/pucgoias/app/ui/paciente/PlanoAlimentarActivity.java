package br.edu.pucgoias.app.ui.paciente;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.Refeicao;

/** Figma: "Meu Plano Alimentar" – o paciente marca as refeições que já fez. */
public class PlanoAlimentarActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);
        configurarHeader(R.string.meu_plano_alimentar);

        LinearLayout container = findViewById(R.id.container);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Refeicao refeicao : MockData.getPacienteLogado().getPlano()) {
            View card = inflater.inflate(R.layout.item_refeicao_paciente, container, false);
            ((TextView) card.findViewById(R.id.tvHorario)).setText(refeicao.getTitulo());

            StringBuilder alimentos = new StringBuilder();
            for (String a : refeicao.getAlimentos()) {
                if (alimentos.length() > 0) alimentos.append('\n');
                alimentos.append("• ").append(a);
            }
            ((TextView) card.findViewById(R.id.tvAlimentos)).setText(alimentos);

            ImageView ivCheck = card.findViewById(R.id.ivCheck);
            atualizarCheck(ivCheck, refeicao.isConcluida());
            View.OnClickListener alternar = v -> {
                refeicao.setConcluida(!refeicao.isConcluida());
                atualizarCheck(ivCheck, refeicao.isConcluida());
            };
            ivCheck.setOnClickListener(alternar);
            card.setOnClickListener(alternar);

            container.addView(card);
        }
    }

    static void atualizarCheck(ImageView iv, boolean marcado) {
        iv.setImageResource(marcado ? R.drawable.bg_check_checked : R.drawable.bg_circle_unchecked);
    }
}
