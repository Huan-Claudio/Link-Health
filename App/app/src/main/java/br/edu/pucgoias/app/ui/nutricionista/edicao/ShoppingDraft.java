package br.edu.pucgoias.app.ui.nutricionista.edicao;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import br.edu.pucgoias.app.data.remote.ShoppingApi;

/** Campos independentes: não tenta adivinhar unidade/quantidade a partir de uma descrição. */
public final class ShoppingDraft {
    public static final String[] UNITS = {"UNIT", "GRAM", "KILOGRAM", "MILLILITER", "LITER", "PACKAGE", "BOX"};
    public static final String[] LABELS = {"Unidade", "Grama (g)", "Quilograma (kg)", "Mililitro (ml)", "Litro (L)", "Pacote", "Caixa"};
    public UUID id;
    public String name = "";
    public String quantity = "1";
    public String unit = "UNIT";
    public boolean purchased;
    public boolean pending;

    public ShoppingDraft() { }
    public ShoppingDraft(ShoppingApi.Item item) { accept(item); }
    public void accept(ShoppingApi.Item item) {
        id = item.getId(); name = item.getName(); quantity = item.getQuantity();
        unit = item.getUnit(); purchased = item.isPurchased();
    }
    public BigDecimal parsedQuantity() {
        BigDecimal value = new BigDecimal(quantity.trim().replace(',', '.'));
        if (value.compareTo(new BigDecimal("0.001")) < 0 || value.scale() > 3
                || value.compareTo(new BigDecimal("999999999.999")) > 0) {
            throw new IllegalArgumentException("Informe uma quantidade positiva, com até 3 casas decimais.");
        }
        return value;
    }
    public String validationError() {
        if (name.trim().isEmpty() || name.trim().length() > 120) return "Informe um nome com até 120 caracteres.";
        try { parsedQuantity(); }
        catch (IllegalArgumentException e) { return "Informe uma quantidade positiva, com até 3 casas decimais."; }
        if (!Arrays.asList(UNITS).contains(unit)) return "Selecione uma unidade válida.";
        return null;
    }
}
