package br.edu.pucgoias.linkhealth.api.record;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecordsHistoryResponse(LocalDate date, long waterMilliliters, long completedMeals,
        BigDecimal weightKg, long photosCount) { }
