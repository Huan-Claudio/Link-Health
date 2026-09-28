package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealRecordRepository {

    List<MealRecord> findByPatientId(UUID patientId);

    Optional<MealRecord> findByIdAndPatientId(UUID recordId, UUID patientId);

    MealRecord save(MealRecord record);

    void delete(MealRecord record);
}
