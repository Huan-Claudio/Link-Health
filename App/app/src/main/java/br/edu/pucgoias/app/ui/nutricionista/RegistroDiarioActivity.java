package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Map;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.RegistroDia;
import br.edu.pucgoias.app.util.Formatador;

/** Figma: "Registro diário do paciente" – água e refeições feitas por dia. */
public class RegistroDiarioActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);
        configurarHeader(R.string.registro_diario);

        Paciente paciente = MockData.getPaciente(getIntent().getIntExtra(MockData.EXTRA_PACIENTE, 0));
        LinearLayout container = findViewById(R.id.container);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (RegistroDia dia : paciente.getRegistros()) {
            View card = inflater.inflate(R.layout.item_registro_dia, container, false);
            ((TextView) card.findViewById(R.id.tvData)).setText(dia.getData());

            LinearProgressIndicator progress = card.findViewById(R.id.progressAgua);
            progress.setProgressCompat(Formatador.porcentagem(dia.getAguaConsumida(), dia.getMetaAgua()), false);
            ((TextView) card.findViewById(R.id.tvAgua)).setText(getString(R.string.agua_valor,
                    Formatador.litros(dia.getAguaConsumida()), Formatador.litros(dia.getMetaAgua())));

            LinearLayout containerRefeicoes = card.findViewById(R.id.containerRefeicoes);
            for (Map.Entry<String, Boolean> refeicao : dia.getRefeicoes().entrySet()) {
                View linha = inflater.inflate(R.layout.item_compra, containerRefeicoes, false);
                ((TextView) linha.findViewById(R.id.tvDescricao)).setText(refeicao.getKey());
                ImageView status = linha.findViewById(R.id.ivAcao);
                boolean feita = refeicao.getValue();
                status.setImageResource(feita ? R.drawable.ic_check_circle : R.drawable.ic_block);
                status.setImageTintList(getColorStateList(feita ? R.color.lh_green : R.color.lh_red));
                status.setBackground(null);
                containerRefeicoes.addView(linha);
            }
            container.addView(card);
        }
    }
}
