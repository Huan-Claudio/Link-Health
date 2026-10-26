package br.edu.pucgoias.linkhealth.domain;

import java.util.UUID;

/** Serializa alterações relacionadas ao mesmo paciente dentro da transação. */
public interface PatientRecordLock {
    void lock(UUID patientId);
}
