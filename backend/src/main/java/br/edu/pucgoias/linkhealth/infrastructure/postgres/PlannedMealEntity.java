package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.PlannedMeal;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "planned_meals")
public class PlannedMealEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private MealPlanEntity plan;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "meal_time", nullable = false)
    private LocalTime time;

    @Column(nullable = false)
    private int position;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<MealPlanItemEntity> items = new ArrayList<>();

    protected PlannedMealEntity() {
    }

    static PlannedMealEntity from(PlannedMeal meal, MealPlanEntity plan, int position) {
        PlannedMealEntity entity = new PlannedMealEntity();
        entity.id = meal.id();
        entity.plan = plan;
        entity.name = meal.name();
        entity.time = meal.time();
        entity.position = position;
        for (int index = 0; index < meal.items().size(); index++) {
            entity.items.add(MealPlanItemEntity.from(meal.items().get(index), entity, index));
        }
        return entity;
    }

    PlannedMeal toDomain() {
        return new PlannedMeal(id, name, time, items.stream().map(MealPlanItemEntity::toDomain).toList());
    }
}
