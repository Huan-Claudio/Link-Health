package br.edu.pucgoias.linkhealth.api.record;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordsSummaryResponse(LocalDate date, long waterMilliliters, Integer waterGoalMilliliters,
        String objective, String nutritionistName, long completedMeals, int plannedMeals,
        BigDecimal latestWeightKg, long photosCount) { }
