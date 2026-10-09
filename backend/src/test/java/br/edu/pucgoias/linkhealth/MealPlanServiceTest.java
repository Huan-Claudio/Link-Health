package br.edu.pucgoias.linkhealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.domain.MealPlan;
import br.edu.pucgoias.linkhealth.infrastructure.local.InMemoryFollowUpRepository;
import br.edu.pucgoias.linkhealth.infrastructure.local.InMemoryMealPlanRepository;
import br.edu.pucgoias.linkhealth.infrastructure.local.InMemoryUserAccountRepository;
import br.edu.pucgoias.linkhealth.service.FollowUpService;
import br.edu.pucgoias.linkhealth.service.MealPlanService;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MealPlanServiceTest {
    private final UserAccountService accounts = new UserAccountService(new InMemoryUserAccountRepository());
    private final FollowUpService followUps = new FollowUpService(new InMemoryFollowUpRepository(), accounts);
    private final MealPlanService service = new MealPlanService(new InMemoryMealPlanRepository(), followUps);
    private final UUID nutritionistId = accounts.register("Ana", null, "ana@exemplo.com", null,
            "CRN-123", "senha1234", AccountRole.NUTRICIONISTA).id();
    private final UUID patientId = accounts.register("Maria", null, "maria@exemplo.com", null,
            "12345678900", "senha1234", AccountRole.PACIENTE).id();

    @Test
    void requiresAcceptedFollowUpAndCompleteMeals() {
        FollowUp invitation = followUps.invite(nutritionistId, patientId, null, null);
        assertThrows(InvalidRequestException.class, () -> service.createDraft(invitation.id()));

        followUps.respond(invitation.id(), patientId, true);
        MealPlan draft = service.createDraft(invitation.id());
        assertThrows(InvalidRequestException.class, () -> service.activate(draft.id()));

        UUID firstMealId = service.addMeal(draft.id(), "Café da manhã", LocalTime.of(8, 0))
                .meals().get(0).id();
        service.addItem(draft.id(), firstMealId, "Pão integral");
        addMealWithItem(draft.id(), "Almoço", 12);
        addMealWithItem(draft.id(), "Jantar", 19);

        assertTrue(service.activate(draft.id()).active());
        assertThrows(InvalidRequestException.class,
                () -> service.addMeal(draft.id(), "Lanche", LocalTime.of(16, 0)));
    }

    @Test
    void keepsOnlyOneActivePlanAndAllowsDraftEditing() {
        FollowUp invitation = followUps.invite(nutritionistId, patientId, null, null);
        followUps.respond(invitation.id(), patientId, true);
        MealPlan first = service.createDraft(invitation.id());
        addMealWithItem(first.id(), "Café", 8);
        addMealWithItem(first.id(), "Almoço", 12);
        addMealWithItem(first.id(), "Jantar", 19);
        service.activate(first.id());

        MealPlan second = service.createDraft(invitation.id());
        UUID mealId = addMealWithItem(second.id(), "Café", 8);
        UUID itemId = service.find(second.id()).meals().get(0).items().get(0).id();
        service.updateMeal(second.id(), mealId, "Café da manhã", LocalTime.of(7, 30));
        service.updateItem(second.id(), mealId, itemId, "Fruta");
        assertEquals("Fruta", service.find(second.id()).meals().get(0).items().get(0).description());
        addMealWithItem(second.id(), "Almoço", 12);
        addMealWithItem(second.id(), "Jantar", 19);
        service.activate(second.id());

        assertFalse(service.find(first.id()).active());
        assertEquals(1, service.byFollowUp(invitation.id()).stream().filter(MealPlan::active).count());
        assertThrows(InvalidRequestException.class, () -> service.deleteDraft(second.id()));
    }

    private UUID addMealWithItem(UUID planId, String name, int hour) {
        MealPlan plan = service.addMeal(planId, name, LocalTime.of(hour, 0));
        UUID mealId = plan.meals().get(plan.meals().size() - 1).id();
        service.addItem(planId, mealId, "Alimento");
        return mealId;
    }
}
