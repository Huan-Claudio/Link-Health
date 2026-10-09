package br.edu.pucgoias.linkhealth.api.followup;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InviteRequest(
        @NotNull UUID nutritionistId,
        @NotNull UUID patientId,
        String objective,
        Integer waterGoalMl) {
}
