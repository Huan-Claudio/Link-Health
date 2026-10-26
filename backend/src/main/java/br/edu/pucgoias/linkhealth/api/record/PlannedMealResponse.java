package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.domain.PlannedMeal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PlannedMealResponse(UUID id, String name, LocalTime time, List<String> foods) {
    public static PlannedMealResponse from(PlannedMeal meal) {
        return new PlannedMealResponse(meal.id(), meal.name(), meal.time(),
                meal.items().stream().map(i -> i.description()).toList());
    }
}
