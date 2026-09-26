package br.edu.pucgoias.linkhealth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShoppingItem(
        UUID id,
        UUID patientId,
        String name,
        BigDecimal quantity,
        MeasurementUnit unit,
        boolean purchased,
        Instant createdAt,
        Instant updatedAt) {
}
