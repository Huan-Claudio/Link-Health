package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.PatientRecordLock;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalPatientRecordLock implements PatientRecordLock {
    // No perfil local, os métodos de escrita dos serviços são sincronizados.
    @Override public void lock(UUID patientId) { }
}
