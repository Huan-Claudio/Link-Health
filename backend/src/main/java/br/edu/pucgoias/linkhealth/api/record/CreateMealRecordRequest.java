package br.edu.pucgoias.linkhealth.api.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateMealRecordRequest(
        UUID mealPlanItemId,
        @NotBlank(message = "O nome da refeição é obrigatório.")
        @Size(max = 120, message = "O nome da refeição deve ter no máximo 120 caracteres.")
        String mealName,
        @PastOrPresent(message = "O horário da refeição não pode estar no futuro.")
        Instant consumedAt,
        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String notes) {
}
