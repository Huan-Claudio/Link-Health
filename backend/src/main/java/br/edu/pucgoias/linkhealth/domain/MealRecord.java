package br.edu.pucgoias.linkhealth.domain;

import java.time.Instant;
import java.util.UUID;

public record MealRecord(
        UUID id,
        UUID patientId,
        UUID mealPlanItemId,
        String mealName,
        Instant consumedAt,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
