package br.edu.pucgoias.linkhealth.api.record;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AdjustWaterIntakeRequest(
        @NotNull @Min(-10000) @Max(10000) Integer amountMilliliters,
        @NotNull LocalDate date,
        String zoneId) { }
