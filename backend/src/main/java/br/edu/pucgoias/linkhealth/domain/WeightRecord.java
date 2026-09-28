package br.edu.pucgoias.linkhealth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeightRecord(
        UUID id,
        UUID patientId,
        BigDecimal weightKg,
        Instant measuredAt,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
