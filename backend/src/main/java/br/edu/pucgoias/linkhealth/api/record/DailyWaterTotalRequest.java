package br.edu.pucgoias.linkhealth.api.record;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record DailyWaterTotalRequest(
        @NotNull @Min(0) @Max(50000) Integer amountMilliliters,
        @NotNull LocalDate date,
        String zoneId) {}
