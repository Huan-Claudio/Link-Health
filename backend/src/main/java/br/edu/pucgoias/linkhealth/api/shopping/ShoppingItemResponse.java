package br.edu.pucgoias.linkhealth.api.shopping;

import br.edu.pucgoias.linkhealth.domain.MeasurementUnit;
import br.edu.pucgoias.linkhealth.domain.ShoppingItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ShoppingItemResponse(
        UUID id,
        UUID patientId,
        String name,
        BigDecimal quantity,
        MeasurementUnit unit,
        boolean purchased,
        Instant createdAt,
        Instant updatedAt) {

    static ShoppingItemResponse from(ShoppingItem item) {
        return new ShoppingItemResponse(
                item.id(), item.patientId(), item.name(), item.quantity(), item.unit(), item.purchased(),
                item.createdAt(), item.updatedAt());
    }
}
