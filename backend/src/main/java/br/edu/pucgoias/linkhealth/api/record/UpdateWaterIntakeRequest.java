package br.edu.pucgoias.linkhealth.api.record;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import java.time.Instant;

public record UpdateWaterIntakeRequest(
        @Min(value = 1, message = "A quantidade de água deve ser maior que zero.")
        @Max(value = 10000, message = "A quantidade de água deve ser de no máximo 10000 ml.")
        Integer amountMilliliters,
        @PastOrPresent(message = "O horário de consumo não pode estar no futuro.")
        Instant consumedAt) {
}
