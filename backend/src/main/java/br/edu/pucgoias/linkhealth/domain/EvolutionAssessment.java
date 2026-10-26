package br.edu.pucgoias.linkhealth.domain;

import java.time.Instant;
import java.util.UUID;

public record EvolutionAssessment(
        UUID id,
        UUID patientId,
        UUID weightRecordId,
        UUID frontPhotoId,
        UUID sidePhotoId,
        UUID backPhotoId,
        Instant recordedAt,
        String requestDigest) {}
