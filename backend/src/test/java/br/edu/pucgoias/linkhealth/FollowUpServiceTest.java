package br.edu.pucgoias.linkhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.domain.FollowUpStatus;
import br.edu.pucgoias.linkhealth.infrastructure.local.InMemoryFollowUpRepository;
import br.edu.pucgoias.linkhealth.infrastructure.local.InMemoryUserAccountRepository;
import br.edu.pucgoias.linkhealth.service.FollowUpService;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FollowUpServiceTest {
    private final UserAccountService accounts = new UserAccountService(new InMemoryUserAccountRepository());
    private final FollowUpService service = new FollowUpService(new InMemoryFollowUpRepository(), accounts);
    private final UUID nutritionistId = accounts.register("Ana", null, "ana@exemplo.com", null,
            "CRN-123", "senha1234", AccountRole.NUTRICIONISTA).id();
    private final UUID patientId = accounts.register("Maria", null, "maria@exemplo.com", null,
            "12345678900", "senha1234", AccountRole.PACIENTE).id();

    @Test
    void invitesAndAcceptsPatient() {
        FollowUp invitation = service.invite(nutritionistId, patientId, "Melhorar alimentação", 2000);

        assertEquals(FollowUpStatus.PENDENTE, invitation.status());
        assertEquals(FollowUpStatus.ATIVO, service.respond(invitation.id(), patientId, true).status());
        assertEquals(1, service.byNutritionist(nutritionistId).size());
        assertEquals(1, service.byPatient(patientId).size());
        assertThrows(InvalidRequestException.class, () -> service.respond(invitation.id(), patientId, false));
    }

    @Test
    void preventsDuplicateAndWrongPatientResponse() {
        FollowUp invitation = service.invite(nutritionistId, patientId, null, null);

        assertThrows(InvalidRequestException.class,
                () -> service.invite(nutritionistId, patientId, null, null));
        assertThrows(InvalidRequestException.class,
                () -> service.respond(invitation.id(), UUID.randomUUID(), true));
        assertEquals(FollowUpStatus.RECUSADO, service.respond(invitation.id(), patientId, false).status());
        assertEquals(FollowUpStatus.PENDENTE,
                service.invite(nutritionistId, patientId, null, null).status());
    }

    @Test
    void updatesAndDeactivatesOnlyTheNutritionistsActiveFollowUp() {
        FollowUp invitation = service.invite(nutritionistId, patientId, null, null);
        UUID otherNutritionistId = accounts.register("Beatriz", null, "bia@exemplo.com", null,
                "CRN-456", "senha1234", AccountRole.NUTRICIONISTA).id();

        assertThrows(InvalidRequestException.class,
                () -> service.updateDetails(invitation.id(), nutritionistId, "Novo objetivo", 1800));
        service.respond(invitation.id(), patientId, true);
        assertThrows(InvalidRequestException.class,
                () -> service.updateDetails(invitation.id(), otherNutritionistId, "Novo objetivo", 1800));
        assertThrows(InvalidRequestException.class,
                () -> service.updateDetails(invitation.id(), nutritionistId, "Novo objetivo", -1));

        FollowUp updated = service.updateDetails(invitation.id(), nutritionistId, " Novo objetivo ", 1800);
        assertEquals("Novo objetivo", updated.objective());
        assertEquals(1800, updated.waterGoalMl());
        assertThrows(InvalidRequestException.class,
                () -> service.deactivate(invitation.id(), otherNutritionistId));
        assertEquals(FollowUpStatus.INATIVO, service.deactivate(invitation.id(), nutritionistId).status());
        assertThrows(InvalidRequestException.class,
                () -> service.updateDetails(invitation.id(), nutritionistId, "Outra meta", 2000));
        assertEquals(FollowUpStatus.PENDENTE,
                service.invite(nutritionistId, patientId, null, null).status());
    }
}
