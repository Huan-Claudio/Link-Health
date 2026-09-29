package br.edu.pucgoias.linkhealth.api.mealplan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MealItemRequest(@NotBlank @Size(max = 150) String description) {
}
