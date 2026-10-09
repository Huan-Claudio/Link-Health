package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.MealItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "meal_plan_items")
public class MealPlanItemEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_id", nullable = false)
    private PlannedMealEntity meal;

    @Column(nullable = false, length = 150)
    private String description;

    @Column(nullable = false)
    private int position;

    protected MealPlanItemEntity() {
    }

    static MealPlanItemEntity from(MealItem item, PlannedMealEntity meal, int position) {
        MealPlanItemEntity entity = new MealPlanItemEntity();
        entity.id = item.id();
        entity.meal = meal;
        entity.description = item.description();
        entity.position = position;
        return entity;
    }

    MealItem toDomain() {
        return new MealItem(id, description);
    }
}
