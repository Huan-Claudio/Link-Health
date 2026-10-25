package br.edu.pucgoias.linkhealth.api.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.hibernate.validator.constraints.URL;

public record UpdateRecommendedProductRequest(
        @Size(min = 1, max = 160, message = "O nome do produto deve ter entre 1 e 160 caracteres.")
        @Pattern(regexp = "(?s).*\\S.*", message = "O nome do produto não pode conter somente espaços.")
        String name,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description,
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
        @Digits(integer = 10, fraction = 2, message = "O preço deve ter até 10 dígitos inteiros e 2 casas decimais.")
        BigDecimal price,
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "A moeda deve ter três letras, por exemplo BRL.")
        String currency,
        @URL(message = "O link de compra deve ser uma URL válida.")
        @Size(max = 2048, message = "O link de compra deve ter no máximo 2048 caracteres.")
        String purchaseUrl) {
}
