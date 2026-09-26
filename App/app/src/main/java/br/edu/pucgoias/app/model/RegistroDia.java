package br.edu.pucgoias.app.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registro diário do paciente: água ingerida e refeições feitas/não feitas. */
public class RegistroDia {
    private final String data;
    private final double aguaConsumida;
    private final double metaAgua;
    private final Map<String, Boolean> refeicoes = new LinkedHashMap<>();

    public RegistroDia(String data, double aguaConsumida, double metaAgua,
                       boolean cafe, boolean almoco, boolean lanche, boolean jantar) {
        this.data = data;
        this.aguaConsumida = aguaConsumida;
        this.metaAgua = metaAgua;
        refeicoes.put("Café da manhã", cafe);
        refeicoes.put("almoço", almoco);
        refeicoes.put("lanche", lanche);
        refeicoes.put("jantar", jantar);
    }

    public String getData() { return data; }
    public double getAguaConsumida() { return aguaConsumida; }
    public double getMetaAgua() { return metaAgua; }
    public Map<String, Boolean> getRefeicoes() { return refeicoes; }
}
