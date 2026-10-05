package br.edu.pucgoias.app.ui.nutricionista.edicao;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.model.Refeicao;

/** Cópia editável de uma refeição: só vai para o plano do paciente ao tocar em "Salvar Alterações". */
public class RascunhoRefeicao {

    public final Refeicao original;
    public String nome;
    public String horario;
    public final List<CampoTexto> alimentos = new ArrayList<>();

    public RascunhoRefeicao(Refeicao original, String[] tiposRefeicao) {
        this.original = original;
        recarregar(tiposRefeicao);
    }

    /** Copia os dados da refeição salva (padronizando "Café da Manhã" -> "Café da manhã"). */
    public final void recarregar(String[] tiposRefeicao) {
        nome = padronizarTipo(original.getNome(), tiposRefeicao);
        horario = original.getHorario();
        alimentos.clear();
        for (String alimento : original.getAlimentos()) alimentos.add(new CampoTexto(alimento));
    }

    /** Aplica o rascunho na refeição do paciente (alimentos em branco são descartados). */
    public void aplicar(String[] tiposRefeicao) {
        original.setNome(nome);
        original.setHorario(horario);
        original.getAlimentos().clear();
        for (CampoTexto alimento : alimentos) {
            if (!alimento.isVazio()) original.getAlimentos().add(alimento.texto.trim());
        }
        recarregar(tiposRefeicao);
    }

    private static String padronizarTipo(String nome, String[] tiposRefeicao) {
        for (String tipo : tiposRefeicao) {
            if (tipo.equalsIgnoreCase(nome.trim())) return tipo;
        }
        return nome;
    }
}
