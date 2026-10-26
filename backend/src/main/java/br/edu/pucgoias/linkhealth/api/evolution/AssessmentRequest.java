package br.edu.pucgoias.linkhealth.api.evolution;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AssessmentRequest(
        @NotNull UUID requestId,
        @NotNull @DecimalMin("1") @DecimalMax("500") @Digits(integer = 3, fraction = 2)
                BigDecimal weightKg,
        @PastOrPresent Instant recordedAt) {}
