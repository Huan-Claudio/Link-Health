package br.edu.pucgoias.app.ui.nutricionista.edicao;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Guarda os rascunhos das telas de edição (plano, lista de compras e produtos).
 * Um ViewModel sobrevive à recriação da Activity (girar a tela, trocar o tema),
 * então o que foi digitado e ainda não foi salvo não se perde.
 */
public class EdicaoViewModel extends ViewModel {

    public final List<RascunhoRefeicao> refeicoes = new ArrayList<>();
    public final List<LinhaCompra> linhasCompra = new ArrayList<>();
    public final List<RascunhoProduto> produtos = new ArrayList<>();
    /** CPFs que já receberam convite na tela de busca. */
    public final List<String> convidados = new ArrayList<>();

    private boolean carregado;

    /** true só na primeira vez: a tela deve preencher os rascunhos a partir dos dados salvos. */
    public boolean precisaCarregar() {
        if (carregado) return false;
        carregado = true;
        return true;
    }
}
