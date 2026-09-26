package br.edu.pucgoias.app.data;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.model.FotoEvolucao;
import br.edu.pucgoias.app.model.ItemCompra;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.model.Produto;
import br.edu.pucgoias.app.model.Refeicao;
import br.edu.pucgoias.app.model.RegistroDia;

/**
 * Dados de exemplo (os mesmos do Figma) mantidos em memória.
 * As telas leem e alteram estas listas, então as mudanças valem enquanto o app estiver aberto.
 * TODO: trocar por chamadas ao back-end / banco MySQL.
 */
public final class MockData {

    public static final String NOME_NUTRICIONISTA = "Dr. Welington";
    public static final String NOME_NUTRICIONISTA_COMPLETO = "Dr. Welington Júlio";
    public static final String NOME_PACIENTE_LOGADO = "Jorge Luiz";

    private static Paciente pacienteLogado;
    private static List<Paciente> pacientes;
    private static List<Paciente> pacientesSemNutricionista;
    private static List<String> convites;

    private MockData() {
    }

    /** Paciente usado nas telas do perfil "Paciente". */
    public static Paciente getPacienteLogado() {
        if (pacienteLogado == null) {
            pacienteLogado = criarPaciente(NOME_PACIENTE_LOGADO, "Ganho de Massa Muscular",
                    "123.456.789-00", "jorge@exemplo.com");
        }
        return pacienteLogado;
    }

    /** Extra usado entre telas para indicar qual paciente abrir (-1 = paciente logado). */
    public static final String EXTRA_PACIENTE = "extra_paciente";

    /** Retorna o paciente pelo índice na lista do nutricionista, ou o paciente logado se -1. */
    public static Paciente getPaciente(int indice) {
        if (indice < 0 || indice >= getPacientes().size()) return getPacienteLogado();
        return getPacientes().get(indice);
    }

    /** Pacientes do nutricionista logado ("Meus Pacientes"). */
    public static List<Paciente> getPacientes() {
        if (pacientes == null) {
            pacientes = new ArrayList<>();
            pacientes.add(criarPaciente("João Silva", "Ganho de Massa Muscular", "111.111.111-11", "joao@exemplo.com"));
            pacientes.add(criarPaciente("Maria Oliveira", "Emagrecimento", "222.222.222-22", "maria.o@exemplo.com"));
            pacientes.add(criarPaciente("Maria José", "Emagrecimento", "333.333.333-33", "maria.j@exemplo.com"));
            pacientes.add(criarPaciente("José Maria", "Emagrecimento", "444.444.444-44", "jose@exemplo.com"));
            pacientes.add(criarPaciente("Enzo Gabriel", "Emagrecimento", "555.555.555-55", "enzo@exemplo.com"));
            pacientes.add(criarPaciente("Gael Pinheiro", "Emagrecimento", "666.666.666-66", "gael@exemplo.com"));
            pacientes.add(criarPaciente("Valentina Rossi", "Emagrecimento", "777.777.777-77", "valentina@exemplo.com"));
        }
        return pacientes;
    }

    /** Pacientes cadastrados que ainda não têm nutricionista (tela de busca por CPF/e-mail). */
    public static List<Paciente> getPacientesSemNutricionista() {
        if (pacientesSemNutricionista == null) {
            pacientesSemNutricionista = new ArrayList<>();
            pacientesSemNutricionista.add(getPacienteLogado());
            pacientesSemNutricionista.add(criarPaciente("Ana Beatriz", "Reeducação alimentar", "888.888.888-88", "ana@exemplo.com"));
            pacientesSemNutricionista.add(criarPaciente("Lucas Almeida", "Ganho de Massa Muscular", "999.999.999-99", "lucas@exemplo.com"));
        }
        return pacientesSemNutricionista;
    }

    /** Convites recebidos pelo paciente logado (nomes dos nutricionistas). */
    public static List<String> getConvites() {
        if (convites == null) {
            convites = new ArrayList<>();
            convites.add(NOME_NUTRICIONISTA);
        }
        return convites;
    }

    private static Paciente criarPaciente(String nome, String objetivo, String cpf, String email) {
        Paciente p = new Paciente(nome, objetivo, cpf, email);
        p.setAguaConsumida(2.0);
        p.setMetaAgua(5.0);

        Refeicao cafe = new Refeicao("Café da Manhã", "08:00",
                "3 ovos cozidos", "2 fatias de pão integral", "100g de mamão");
        cafe.setConcluida(true);
        p.getPlano().add(cafe);
        p.getPlano().add(new Refeicao("Almoço", "12:00",
                "150g de peito de frango grelhado", "200g de arroz branco ou integral",
                "100g de feijão carioca", "Salada de folhas verdes à vontade"));
        p.getPlano().add(new Refeicao("Lanche", "15:00",
                "1 copo de iogurte natural com 30g de whey", "100g de mamão com sementes de chia",
                "120g de castanhas ou amendoim"));
        p.getPlano().add(new Refeicao("Jantar", "20:00",
                "150g de patinho moído ou tiras de carne", "200g de purê de batata doce ou mandioca",
                "Brócolis cozido no vapor à vontade"));

        String[] itens = {"3 caixas de ovos", "2 pacotes de pão integral", "1 mamão",
                "5 pacotes de arroz", "6 pacotes de feijão", "2,5kg de peito de frango",
                "3 pacotes de semente de chia", "Brócolis", "28 potinhos de iogurte natural"};
        for (String item : itens) p.getListaCompras().add(new ItemCompra(item));
        p.getListaCompras().get(0).setComprado(true);

        p.getProdutos().add(new Produto("Whey Protein Growth", "129,90",
                "https://www.mercadolivre.com.br"));
        p.getProdutos().add(new Produto("Creatina Monohidratada Growth", "89,90",
                "https://www.mercadolivre.com.br"));

        for (int i = 0; i < 4; i++) p.getFotos().add(new FotoEvolucao("11/09/2026", 70, null));

        p.getRegistros().add(new RegistroDia("11/09/2026", 2.0, 5.0, true, true, false, true));
        p.getRegistros().add(new RegistroDia("10/09/2026", 4.5, 5.0, true, true, true, true));
        p.getRegistros().add(new RegistroDia("09/09/2026", 3.0, 5.0, true, false, true, true));
        return p;
    }
}
