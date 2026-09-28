package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.List;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Refeicao;
import br.edu.pucgoias.app.util.TextoAlterado;

public class EditarPlanoActivity extends BaseActivity {

    private Paciente paciente;
    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);

        paciente = MockData.getPaciente(getIntent().getIntExtra(MockData.EXTRA_PACIENTE, 0));
        configurarHeader(getString(R.string.plano_alimentar_de, paciente.getNome()));
        container = findViewById(R.id.container);

        Button btnRodape = findViewById(R.id.btnRodape);
        btnRodape.setVisibility(View.VISIBLE);
        btnRodape.setText(R.string.adicionar_refeicao);
        btnRodape.setOnClickListener(v -> {
            paciente.getPlano().add(new Refeicao(getString(R.string.nova_refeicao), "00:00"));
            montarPlano();
        });

        montarPlano();
    }

    private void montarPlano() {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Refeicao refeicao : paciente.getPlano()) {
            View card = inflater.inflate(R.layout.item_refeicao_editavel, container, false);

            EditText etNome = card.findViewById(R.id.etNomeRefeicao);
            etNome.setText(refeicao.getRotuloEdicao());
            etNome.addTextChangedListener(new TextoAlterado(refeicao::setRotuloEdicao));

            card.findViewById(R.id.btnRemoverRefeicao).setOnClickListener(v -> {
                paciente.getPlano().remove(refeicao);
                montarPlano();
            });

            LinearLayout containerAlimentos = card.findViewById(R.id.containerAlimentos);
            montarAlimentos(containerAlimentos, refeicao.getAlimentos());

            card.findViewById(R.id.btnAdicionarAlimento).setOnClickListener(v -> {
                refeicao.getAlimentos().add("");
                montarAlimentos(containerAlimentos, refeicao.getAlimentos());
                // Foca o novo campo para digitar direto.
                View ultimo = containerAlimentos.getChildAt(containerAlimentos.getChildCount() - 1);
                ultimo.findViewById(R.id.etAlimento).requestFocus();
            });

            container.addView(card);
        }
    }

    // TODO: fazer botão "Salvar" para cada refeição
    private void montarAlimentos(LinearLayout containerAlimentos, List<String> alimentos) {
        containerAlimentos.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < alimentos.size(); i++) {
            final int indice = i;
            View linha = inflater.inflate(R.layout.item_alimento_editavel, containerAlimentos, false);
            EditText et = linha.findViewById(R.id.etAlimento);
            et.setText(alimentos.get(i));
            et.addTextChangedListener(new TextoAlterado(texto -> alimentos.set(indice, texto)));
            linha.findViewById(R.id.btnRemoverAlimento).setOnClickListener(v -> {
                alimentos.remove(indice);
                montarAlimentos(containerAlimentos, alimentos);
            });
            containerAlimentos.addView(linha);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove alimentos deixados em branco.
        for (Refeicao r : paciente.getPlano()) {
            r.getAlimentos().removeIf(a -> a.trim().isEmpty());
        }
    }
}
