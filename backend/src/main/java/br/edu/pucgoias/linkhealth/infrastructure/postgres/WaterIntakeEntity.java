package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.WaterIntake;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "water_intakes")
public class WaterIntakeEntity {

    @Id
    private UUID id;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "amount_milliliters", nullable = false)
    private int amountMilliliters;
    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WaterIntakeEntity() {
    }

    private WaterIntakeEntity(WaterIntake intake) {
        this.id = intake.id();
        this.patientId = intake.patientId();
        this.amountMilliliters = intake.amountMilliliters();
        this.consumedAt = intake.consumedAt();
        this.createdAt = intake.createdAt();
        this.updatedAt = intake.updatedAt();
    }

    static WaterIntakeEntity from(WaterIntake intake) {
        return new WaterIntakeEntity(intake);
    }

    WaterIntake toDomain() {
        return new WaterIntake(id, patientId, amountMilliliters, consumedAt, createdAt, updatedAt);
    }
}
