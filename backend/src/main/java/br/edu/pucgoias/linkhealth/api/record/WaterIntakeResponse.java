package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.domain.WaterIntake;
import java.time.Instant;
import java.util.UUID;

public record WaterIntakeResponse(
        UUID id,
        UUID patientId,
        int amountMilliliters,
        Instant consumedAt,
        Instant createdAt,
        Instant updatedAt) {

    static WaterIntakeResponse from(WaterIntake intake) {
        return new WaterIntakeResponse(
                intake.id(), intake.patientId(), intake.amountMilliliters(), intake.consumedAt(),
                intake.createdAt(), intake.updatedAt());
    }
}
