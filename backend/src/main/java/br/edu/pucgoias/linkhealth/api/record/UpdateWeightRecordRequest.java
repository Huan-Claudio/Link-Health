package br.edu.pucgoias.linkhealth.api.record;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record UpdateWeightRecordRequest(
        @DecimalMin(value = "1.00", message = "O peso deve ser maior ou igual a 1 kg.")
        @DecimalMax(value = "500.00", message = "O peso deve ser menor ou igual a 500 kg.")
        BigDecimal weightKg,
        @PastOrPresent(message = "A data da medição não pode estar no futuro.")
        Instant measuredAt,
        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String notes) {
}
