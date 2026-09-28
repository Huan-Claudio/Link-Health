package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.domain.WeightRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeightRecordResponse(
        UUID id,
        UUID patientId,
        BigDecimal weightKg,
        Instant measuredAt,
        String notes,
        Instant createdAt,
        Instant updatedAt) {

    static WeightRecordResponse from(WeightRecord record) {
        return new WeightRecordResponse(
                record.id(), record.patientId(), record.weightKg(), record.measuredAt(), record.notes(),
                record.createdAt(), record.updatedAt());
    }
}
