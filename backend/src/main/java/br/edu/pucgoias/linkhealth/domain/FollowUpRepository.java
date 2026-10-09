package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowUpRepository {
    Optional<FollowUp> findById(UUID id);

    List<FollowUp> findByNutritionistId(UUID nutritionistId);

    List<FollowUp> findByPatientId(UUID patientId);

    FollowUp save(FollowUp followUp);
}
