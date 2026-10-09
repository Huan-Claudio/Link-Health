package br.edu.pucgoias.app.model;

import java.util.ArrayList;
import java.util.List;

/** Uma refeição do plano alimentar (ex.: 08:00 - Café da Manhã). */
public class Refeicao {

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

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getHorario() { return horario; }
    public void setHorario(String horario) { this.horario = horario; }
    public List<String> getAlimentos() { return alimentos; }
    public boolean isConcluida() { return concluida; }
    public void setConcluida(boolean concluida) { this.concluida = concluida; }
}
