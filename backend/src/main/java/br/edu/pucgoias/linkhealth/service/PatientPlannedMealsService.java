package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.domain.FollowUpRepository;
import br.edu.pucgoias.linkhealth.domain.FollowUpStatus;
import br.edu.pucgoias.linkhealth.domain.MealPlan;
import br.edu.pucgoias.linkhealth.domain.MealPlanRepository;
import br.edu.pucgoias.linkhealth.domain.PlannedMeal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Consulta os planos já publicados; não cria ou modifica prescrições. */
@Service
public class PatientPlannedMealsService {
    private final FollowUpRepository followUps;
    private final MealPlanRepository plans;
    private final UserAccountService accounts;
    private final PatientAccessService access;

    public PatientPlannedMealsService(FollowUpRepository followUps, MealPlanRepository plans,
            UserAccountService accounts, PatientAccessService access) {
        this.followUps = followUps;
        this.plans = plans;
        this.accounts = accounts;
        this.access = access;
    }

    public List<PlannedMeal> list(UUID patientId) {
        access.ensureCanAccess(patientId);
        accounts.requirePatient(patientId);
        return followUps.findByPatientId(patientId).stream()
                .filter(f -> f.status() == FollowUpStatus.ATIVO)
                .flatMap(f -> plans.findByFollowUpId(f.id()).stream())
                .filter(MealPlan::active).flatMap(p -> p.meals().stream())
                .sorted(Comparator.comparing(PlannedMeal::time).thenComparing(PlannedMeal::id)).toList();
    }
}
