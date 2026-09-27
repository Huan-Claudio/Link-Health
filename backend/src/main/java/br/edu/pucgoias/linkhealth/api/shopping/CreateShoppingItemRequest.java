package br.edu.pucgoias.linkhealth.api.shopping;

import br.edu.pucgoias.linkhealth.domain.MeasurementUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateShoppingItemRequest(
        @NotBlank(message = "O nome do item é obrigatório.")
        @Size(max = 120, message = "O nome do item deve ter no máximo 120 caracteres.")
        String name,
        @NotNull(message = "A quantidade é obrigatória.")
        @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,
        @NotNull(message = "A unidade é obrigatória.")
        MeasurementUnit unit) {
}
