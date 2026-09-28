package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.domain.MealRecord;
import java.time.Instant;
import java.util.UUID;

public record MealRecordResponse(
        UUID id,
        UUID patientId,
        UUID mealPlanItemId,
        String mealName,
        Instant consumedAt,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    static MealRecordResponse from(MealRecord record) {
        return new MealRecordResponse(
                record.id(), record.patientId(), record.mealPlanItemId(), record.mealName(), record.consumedAt(),
                record.notes(), record.createdAt(), record.updatedAt());
    }
}
