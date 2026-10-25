package br.edu.pucgoias.app;

import br.edu.pucgoias.app.model.Produto;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProdutoTest {
    @Test public void formatsRealPriceWithTwoDecimalPlaces() {
        Produto produto = new Produto("Teste", "19.5", "");
        assertTrue(produto.getPrecoFormatado().contains("R$"));
        assertTrue(produto.getPrecoFormatado().endsWith("19,50"));
    }

    @Test public void preservesForeignCurrencyAndMissingPrice() {
        Produto produto = new Produto("Teste", "19.50", "");
        produto.setMoeda("USD");
        assertFalse(produto.getPrecoFormatado().startsWith("R$"));
        assertTrue(produto.getPrecoFormatado().contains("19,50"));
        produto.setPreco("");
        assertEquals("Preço não informado", produto.getPrecoFormatado());
    }
}
