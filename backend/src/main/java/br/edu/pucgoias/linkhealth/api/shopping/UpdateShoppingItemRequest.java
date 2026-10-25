package br.edu.pucgoias.linkhealth.api.shopping;

import br.edu.pucgoias.linkhealth.domain.MeasurementUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateShoppingItemRequest(
        @Size(min = 1, max = 120, message = "O nome do item deve ter entre 1 e 120 caracteres.")
        @Pattern(regexp = "(?s).*\\S.*", message = "O nome do item não pode conter somente espaços.")
        String name,
        @DecimalMin(value = "0.001", message = "A quantidade deve ser maior que zero.")
        @Digits(integer = 9, fraction = 3, message = "A quantidade deve ter até 9 dígitos inteiros e 3 casas decimais.")
        BigDecimal quantity,
        MeasurementUnit unit) {
}
