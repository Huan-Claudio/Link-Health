package br.edu.pucgoias.linkhealth.domain;

import java.util.*;

public interface EvolutionAssessmentRepository {
    Optional<EvolutionAssessment> findById(UUID id);

    List<EvolutionAssessment> findByPatientId(UUID patientId);

    EvolutionAssessment save(EvolutionAssessment value);

    void delete(EvolutionAssessment value);

    boolean referencesPhoto(UUID id);

    boolean referencesWeight(UUID id);
}
