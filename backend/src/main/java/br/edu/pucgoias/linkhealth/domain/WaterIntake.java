package br.edu.pucgoias.linkhealth.domain;

import java.time.Instant;
import java.util.UUID;

public record WaterIntake(
        UUID id,
        UUID patientId,
        int amountMilliliters,
        Instant consumedAt,
        Instant createdAt,
        Instant updatedAt) {
}
