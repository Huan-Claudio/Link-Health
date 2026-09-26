package br.edu.pucgoias.app.model;

import java.util.ArrayList;
import java.util.List;

/** Paciente e todos os dados que o nutricionista acompanha. */
public class Paciente {
    private String nome;
    private String objetivo;
    private String cpf;
    private String email;

    private double aguaConsumida;
    private double metaAgua = 5.0;

    private final List<Refeicao> plano = new ArrayList<>();
    private final List<ItemCompra> listaCompras = new ArrayList<>();
    private final List<Produto> produtos = new ArrayList<>();
    private final List<FotoEvolucao> fotos = new ArrayList<>();
    private final List<RegistroDia> registros = new ArrayList<>();

    public Paciente(String nome, String objetivo, String cpf, String email) {
        this.nome = nome;
        this.objetivo = objetivo;
        this.cpf = cpf;
        this.email = email;
    }

    /** "João Silva" -> "JS" */
    public String getIniciais() {
        return iniciais(nome);
    }

    public static String iniciais(String nome) {
        String[] partes = nome.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (!p.isEmpty() && Character.isLetter(p.charAt(0)) && !p.endsWith(".")) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
            if (sb.length() == 2) break;
        }
        return sb.toString();
    }

    public int getRefeicoesConcluidas() {
        int total = 0;
        for (Refeicao r : plano) if (r.isConcluida()) total++;
        return total;
    }

    /** Soma (ou subtrai) litros de água, sem ficar negativo. */
    public void adicionarAgua(double litros) {
        aguaConsumida = Math.max(0, aguaConsumida + litros);
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public double getAguaConsumida() { return aguaConsumida; }
    public void setAguaConsumida(double aguaConsumida) { this.aguaConsumida = aguaConsumida; }
    public double getMetaAgua() { return metaAgua; }
    public void setMetaAgua(double metaAgua) { this.metaAgua = metaAgua; }
    public List<Refeicao> getPlano() { return plano; }
    public List<ItemCompra> getListaCompras() { return listaCompras; }
    public List<Produto> getProdutos() { return produtos; }
    public List<FotoEvolucao> getFotos() { return fotos; }
    public List<RegistroDia> getRegistros() { return registros; }
}
