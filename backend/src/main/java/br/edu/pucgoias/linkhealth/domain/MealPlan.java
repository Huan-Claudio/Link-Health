package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.UUID;

public record MealPlan(UUID id, UUID followUpId, boolean active, boolean published, List<PlannedMeal> meals) {
    public MealPlan(UUID id, UUID followUpId, boolean active, List<PlannedMeal> meals) {
        this(id, followUpId, active, active, meals);
    }
    public MealPlan {
        meals = List.copyOf(meals);
    }
}
