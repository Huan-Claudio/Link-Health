package br.edu.pucgoias.linkhealth;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import java.util.UUID;

final class PatientFixtures {
    private PatientFixtures() { }

    static UUID create(UserAccountService accounts) {
        String suffix = UUID.randomUUID().toString();
        return accounts.register("Paciente teste", null, suffix + "@example.org", null,
                suffix.substring(0, 20), "senha1234", AccountRole.PACIENTE).id();
    }
}
