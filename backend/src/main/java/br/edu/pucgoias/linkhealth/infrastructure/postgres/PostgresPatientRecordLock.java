package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.PatientRecordLock;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("postgres")
public class PostgresPatientRecordLock implements PatientRecordLock {
    private final JdbcTemplate jdbc;
    public PostgresPatientRecordLock(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(UUID patientId) {
        jdbc.queryForList("SELECT user_id FROM patients WHERE user_id = ? FOR UPDATE", patientId);
    }
}
