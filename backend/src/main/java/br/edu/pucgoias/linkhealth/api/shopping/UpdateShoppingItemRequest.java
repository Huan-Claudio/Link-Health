package br.edu.pucgoias.linkhealth.api.shopping;

import br.edu.pucgoias.linkhealth.domain.MeasurementUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateShoppingItemRequest(
        @Size(min = 1, max = 120, message = "O nome do item deve ter entre 1 e 120 caracteres.")
        String name,
        @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero.")
        BigDecimal quantity,
        MeasurementUnit unit) {
}
