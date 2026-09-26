package br.edu.pucgoias.linkhealth.api.product;

import br.edu.pucgoias.linkhealth.domain.RecommendedProduct;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecommendedProductResponse(
        UUID id,
        UUID patientId,
        String name,
        String description,
        BigDecimal price,
        String currency,
        String purchaseUrl,
        Instant createdAt,
        Instant updatedAt) {

    static RecommendedProductResponse from(RecommendedProduct product) {
        return new RecommendedProductResponse(
                product.id(), product.patientId(), product.name(), product.description(), product.price(),
                product.currency(), product.purchaseUrl(), product.createdAt(), product.updatedAt());
    }
}
