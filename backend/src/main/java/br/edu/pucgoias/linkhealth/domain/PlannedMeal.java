package br.edu.pucgoias.linkhealth.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PlannedMeal(UUID id, String name, LocalTime time, List<MealItem> items) {
    public PlannedMeal {
        items = List.copyOf(items);
    }
}
