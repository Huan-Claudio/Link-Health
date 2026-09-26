package br.edu.pucgoias.linkhealth.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecommendedProduct(
        UUID id,
        UUID patientId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String purchaseUrl,
        Instant createdAt,
        Instant updatedAt) {
}
