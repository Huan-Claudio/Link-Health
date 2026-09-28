package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterIntakeRepository {

    List<WaterIntake> findByPatientId(UUID patientId);

    Optional<WaterIntake> findByIdAndPatientId(UUID intakeId, UUID patientId);

    WaterIntake save(WaterIntake intake);

    void delete(WaterIntake intake);
}
