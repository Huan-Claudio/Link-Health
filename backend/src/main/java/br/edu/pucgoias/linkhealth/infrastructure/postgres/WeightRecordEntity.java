package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.WeightRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "weight_records")
public class WeightRecordEntity {

    @Id
    private UUID id;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;
    @Column(name = "measured_at", nullable = false)
    private Instant measuredAt;
    @Column(length = 500)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WeightRecordEntity() {
    }

    private WeightRecordEntity(WeightRecord record) {
        this.id = record.id();
        this.patientId = record.patientId();
        this.weightKg = record.weightKg();
        this.measuredAt = record.measuredAt();
        this.notes = record.notes();
        this.createdAt = record.createdAt();
        this.updatedAt = record.updatedAt();
    }

    static WeightRecordEntity from(WeightRecord record) {
        return new WeightRecordEntity(record);
    }

    WeightRecord toDomain() {
        return new WeightRecord(id, patientId, weightKg, measuredAt, notes, createdAt, updatedAt);
    }
}
