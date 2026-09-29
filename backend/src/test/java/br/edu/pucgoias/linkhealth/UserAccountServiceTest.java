package br.edu.pucgoias.linkhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import br.edu.pucgoias.linkhealth.infrastructure.local.InMemoryUserAccountRepository;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import org.junit.jupiter.api.Test;

class UserAccountServiceTest {
    private final UserAccountService service = new UserAccountService(new InMemoryUserAccountRepository());

    @Test
    void registersAndChecksCredentialsWithoutReturningPlainPassword() {
        UserAccount patient = service.register("Maria Silva", null, "MARIA@EXEMPLO.COM", null,
                "123.456.789-00", "senha1234", AccountRole.PACIENTE);

        assertEquals("maria@exemplo.com", patient.email());
        assertFalse(patient.passwordHash().equals("senha1234"));
        assertTrue(patient.passwordHash().startsWith("$2"));
        assertEquals(patient.id(), service.checkCredentials("maria@exemplo.com", "senha1234").id());
        assertThrows(InvalidRequestException.class,
                () -> service.checkCredentials("maria@exemplo.com", "senha-errada"));
    }

    @Test
    void preventsDuplicateEmailAndDocument() {
        service.register("Maria Silva", null, "maria@exemplo.com", null,
                "123.456.789-00", "senha1234", AccountRole.PACIENTE);

        assertThrows(InvalidRequestException.class,
                () -> service.register("Outra Pessoa", null, "MARIA@EXEMPLO.COM", null,
                        "987.654.321-00", "senha1234", AccountRole.PACIENTE));
        assertThrows(InvalidRequestException.class,
                () -> service.register("Outra Pessoa", null, "outra@exemplo.com", null,
                        "123.456.789-00", "senha1234", AccountRole.PACIENTE));
    }

    @Test
    void searchesOnlyPatientsByEmailOrCpf() {
        service.register("Maria Silva", null, "maria@exemplo.com", null,
                "123.456.789-00", "senha1234", AccountRole.PACIENTE);
        service.register("Nutricionista Ana", null, "ana@exemplo.com", null,
                "CRN-123", "senha1234", AccountRole.NUTRICIONISTA);

        assertEquals(1, service.searchPatients("123456").size());
        assertEquals(1, service.searchPatients("maria@").size());
        assertEquals(0, service.searchPatients("ana@").size());
    }
}
