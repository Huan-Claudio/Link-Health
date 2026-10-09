package br.edu.pucgoias.linkhealth.domain;

import java.time.Instant;
import java.util.UUID;

public record FollowUp(
        UUID id,
        UUID nutritionistId,
        UUID patientId,
        FollowUpStatus status,
        String objective,
        Integer waterGoalMl,
        Instant invitedAt) {
}
