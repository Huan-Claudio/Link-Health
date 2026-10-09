package br.edu.pucgoias.linkhealth.api.mealplan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record MealRequest(@NotBlank @Size(max = 50) String name, @NotNull LocalTime time) {
}
