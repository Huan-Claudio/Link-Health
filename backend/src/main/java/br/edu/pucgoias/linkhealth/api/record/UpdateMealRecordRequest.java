package br.edu.pucgoias.linkhealth.api.record;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record UpdateMealRecordRequest(
        @Size(min = 1, max = 120, message = "O nome da refeição deve ter entre 1 e 120 caracteres.")
        String mealName,
        @PastOrPresent(message = "O horário da refeição não pode estar no futuro.")
        Instant consumedAt,
        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String notes) {
}
