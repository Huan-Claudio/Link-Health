package br.edu.pucgoias.linkhealth.api.followup;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record RespondRequest(@NotNull UUID patientId, @NotNull Boolean accept) {
}
