package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeightRecordRepository {

    List<WeightRecord> findByPatientId(UUID patientId);

    Optional<WeightRecord> findByIdAndPatientId(UUID recordId, UUID patientId);

    WeightRecord save(WeightRecord record);

    void delete(WeightRecord record);
}
