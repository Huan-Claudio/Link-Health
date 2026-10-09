package br.edu.pucgoias.app.ui.nutricionista;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Refeicao;
import br.edu.pucgoias.app.util.TextoAlterado;

/**
 * Figma: "Editar Plano Alimentar do paciente".
 * Cada card edita uma cópia da refeição (rascunho); as mudanças só vão para o plano
 * do paciente ao tocar em "Salvar Alterações" daquele card.
 */
public class EditarPlanoActivity extends BaseActivity {

    /** Cópia editável de uma refeição. */
    private static class Rascunho {
        String nome;
        String horario;
        final List<String> alimentos;

        Rascunho(Refeicao refeicao, String nomePadronizado) {
            nome = nomePadronizado;
            horario = refeicao.getHorario();
            alimentos = new ArrayList<>(refeicao.getAlimentos());
        }
    }

    private Paciente paciente;
    private LinearLayout container;
    private String[] tiposRefeicao;
    private final Map<Refeicao, Rascunho> rascunhos = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_simples);

        paciente = MockData.getPaciente(getIntent().getIntExtra(MockData.EXTRA_PACIENTE, 0));
        configurarHeader(getString(R.string.plano_alimentar_de, paciente.getNome()));
        container = findViewById(R.id.container);
        tiposRefeicao = getResources().getStringArray(R.array.tipos_refeicao);

        montarPlano();
    }

    private void montarPlano() {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Refeicao refeicao : paciente.getPlano()) {
            container.addView(criarCard(inflater, refeicao));
        }

        // "+ Adicionar Refeição" fica logo abaixo do último card.
        View btnAdicionarRefeicao = inflater.inflate(R.layout.view_botao_adicionar_refeicao, container, false);
        btnAdicionarRefeicao.setOnClickListener(v -> {
            // TODO: criar a refeição no back-end.
            paciente.getPlano().add(new Refeicao(tiposRefeicao[0], "00:00"));
            montarPlano();
        });
        container.addView(btnAdicionarRefeicao);
    }

    private View criarCard(LayoutInflater inflater, Refeicao refeicao) {
        Rascunho rascunho = rascunhos.get(refeicao);
        if (rascunho == null) {
            rascunho = new Rascunho(refeicao, padronizarTipo(refeicao.getNome()));
            rascunhos.put(refeicao, rascunho);
        }
        final Rascunho r = rascunho;
        View card = inflater.inflate(R.layout.item_refeicao_editavel, container, false);

        TextView tvTipo = card.findViewById(R.id.tvTipoRefeicao);
        tvTipo.setText(r.nome);
        tvTipo.setOnClickListener(v -> escolherTipo(tvTipo, r));

        TextView tvHorario = card.findViewById(R.id.tvHorario);
        tvHorario.setText(r.horario);
        tvHorario.setOnClickListener(v -> escolherHorario(tvHorario, r));

        card.findViewById(R.id.btnRemoverRefeicao).setOnClickListener(v -> {
            // TODO: remover a refeição no back-end.
            paciente.getPlano().remove(refeicao);
            rascunhos.remove(refeicao);
            montarPlano();
        });

        LinearLayout containerAlimentos = card.findViewById(R.id.containerAlimentos);
        montarAlimentos(containerAlimentos, r.alimentos);

        card.findViewById(R.id.btnAdicionarAlimento).setOnClickListener(v -> {
            r.alimentos.add("");
            montarAlimentos(containerAlimentos, r.alimentos);
            // Foca o novo campo para digitar direto.
            View ultimo = containerAlimentos.getChildAt(containerAlimentos.getChildCount() - 1);
            ultimo.findViewById(R.id.etAlimento).requestFocus();
        });

        card.findViewById(R.id.btnSalvarRefeicao).setOnClickListener(v -> salvar(refeicao, r));
        return card;
    }

    /** Dropdown com os tipos de refeição. */
    private void escolherTipo(TextView tvTipo, Rascunho r) {
        PopupMenu popup = new PopupMenu(this, tvTipo);
        for (int i = 0; i < tiposRefeicao.length; i++) {
            popup.getMenu().add(0, i, i, tiposRefeicao[i]);
        }
        popup.setOnMenuItemClickListener(item -> {
            r.nome = tiposRefeicao[item.getItemId()];
            tvTipo.setText(r.nome);
            return true;
        });
        popup.show();
    }

    /** Relógio nativo do Android (24h) para escolher o horário da refeição. */
    private void escolherHorario(TextView tvHorario, Rascunho r) {
        int hora = 0;
        int minuto = 0;
        try {
            String[] partes = r.horario.split(":");
            hora = Integer.parseInt(partes[0].trim());
            minuto = Integer.parseInt(partes[1].trim());
        } catch (RuntimeException ignored) {
            // horário inválido: começa em 00:00
        }
        new TimePickerDialog(this, (view, horaEscolhida, minutoEscolhido) -> {
            r.horario = String.format(Locale.ROOT, "%02d:%02d", horaEscolhida, minutoEscolhido);
            tvHorario.setText(r.horario);
        }, hora, minuto, true).show();
    }

    /** Aplica o rascunho do card na refeição do paciente. */
    private void salvar(Refeicao refeicao, Rascunho r) {
        // TODO: salvar a refeição no back-end.
        refeicao.setNome(r.nome);
        refeicao.setHorario(r.horario);
        refeicao.getAlimentos().clear();
        for (String alimento : r.alimentos) {
            if (!alimento.trim().isEmpty()) refeicao.getAlimentos().add(alimento.trim());
        }
        rascunhos.remove(refeicao);
        toast(R.string.refeicao_salva);
        montarPlano();
    }

    /** Usa a grafia da lista de tipos quando o nome bate (ex.: "Café da Manhã" -> "Café da manhã"). */
    private String padronizarTipo(String nome) {
        for (String tipo : tiposRefeicao) {
            if (tipo.equalsIgnoreCase(nome.trim())) return tipo;
        }
        return nome;
    }

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
}
