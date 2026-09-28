package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.MealRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meal_records")
public class MealRecordEntity {

    @Id
    private UUID id;
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;
    @Column(name = "meal_plan_item_id")
    private UUID mealPlanItemId;
    @Column(name = "meal_name", nullable = false, length = 120)
    private String mealName;
    @Column(name = "consumed_at", nullable = false)
    private Instant consumedAt;
    @Column(length = 500)
    private String notes;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MealRecordEntity() {
    }

    private MealRecordEntity(MealRecord record) {
        this.id = record.id();
        this.patientId = record.patientId();
        this.mealPlanItemId = record.mealPlanItemId();
        this.mealName = record.mealName();
        this.consumedAt = record.consumedAt();
        this.notes = record.notes();
        this.createdAt = record.createdAt();
        this.updatedAt = record.updatedAt();
    }

    static MealRecordEntity from(MealRecord record) {
        return new MealRecordEntity(record);
    }

    MealRecord toDomain() {
        return new MealRecord(id, patientId, mealPlanItemId, mealName, consumedAt, notes, createdAt, updatedAt);
    }
}
