package br.edu.pucgoias.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Uma refeição do plano alimentar (ex.: 08:00 - Café da Manhã). */
public class Refeicao {

    private static final Pattern ROTULO = Pattern.compile("^(.*?)\\s*\\((\\d{1,2}:\\d{2})\\)\\s*$");

    private String nome;
    private String horario;
    private final List<String> alimentos = new ArrayList<>();
    private boolean concluida;

    public Refeicao(String nome, String horario, String... alimentos) {
        this.nome = nome;
        this.horario = horario;
        for (String a : alimentos) this.alimentos.add(a);
    }

    /** Texto do chip verde na tela do paciente: "08:00 - Café da Manhã". */
    public String getTitulo() {
        return horario + " - " + nome;
    }

    /** Texto editável na tela do nutricionista: "Café da manhã (08:00)". */
    public String getRotuloEdicao() {
        return nome + " (" + horario + ")";
    }

    /** Atualiza nome e horário a partir de um texto no formato "Nome (HH:MM)". */
    public void setRotuloEdicao(String rotulo) {
        Matcher m = ROTULO.matcher(rotulo);
        if (m.matches()) {
            nome = m.group(1);
            horario = m.group(2);
        } else {
            nome = rotulo;
        }
    }

    public String getNome() { return nome; }
    public String getHorario() { return horario; }
    public List<String> getAlimentos() { return alimentos; }
    public boolean isConcluida() { return concluida; }
    public void setConcluida(boolean concluida) { this.concluida = concluida; }
}
