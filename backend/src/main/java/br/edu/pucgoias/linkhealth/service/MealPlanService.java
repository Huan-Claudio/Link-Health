package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.FollowUpStatus;
import br.edu.pucgoias.linkhealth.domain.MealItem;
import br.edu.pucgoias.linkhealth.domain.MealPlan;
import br.edu.pucgoias.linkhealth.domain.MealPlanRepository;
import br.edu.pucgoias.linkhealth.domain.PlannedMeal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class MealPlanService {
    private final MealPlanRepository repository;
    private final FollowUpService followUps;

    public MealPlanService(MealPlanRepository repository, FollowUpService followUps) {
        this.repository = repository;
        this.followUps = followUps;
    }

    public MealPlan createDraft(UUID followUpId) {
        requireActiveFollowUp(followUpId);
        return repository.save(new MealPlan(UUID.randomUUID(), followUpId, false, List.of()));
    }

    public List<MealPlan> byFollowUp(UUID followUpId) {
        followUps.find(followUpId);
        return repository.findByFollowUpId(followUpId);
    }

    public MealPlan find(UUID planId) {
        return repository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plano alimentar não encontrado."));
    }

    public synchronized MealPlan addMeal(UUID planId, String name, LocalTime time) {
        MealPlan plan = requireDraft(planId);
        List<PlannedMeal> meals = new ArrayList<>(plan.meals());
        meals.add(new PlannedMeal(UUID.randomUUID(), requiredText(name), time, List.of()));
        return saveMeals(plan, meals);
    }

    public synchronized MealPlan updateMeal(UUID planId, UUID mealId, String name, LocalTime time) {
        MealPlan plan = requireDraft(planId);
        PlannedMeal meal = findMeal(plan, mealId);
        return replaceMeal(plan, new PlannedMeal(meal.id(), requiredText(name), time, meal.items()));
    }

    public synchronized MealPlan removeMeal(UUID planId, UUID mealId) {
        MealPlan plan = requireDraft(planId);
        findMeal(plan, mealId);
        return saveMeals(plan, plan.meals().stream().filter(meal -> !meal.id().equals(mealId)).toList());
    }

    public synchronized MealPlan addItem(UUID planId, UUID mealId, String description) {
        MealPlan plan = requireDraft(planId);
        PlannedMeal meal = findMeal(plan, mealId);
        List<MealItem> items = new ArrayList<>(meal.items());
        items.add(new MealItem(UUID.randomUUID(), requiredText(description)));
        return replaceMeal(plan, new PlannedMeal(meal.id(), meal.name(), meal.time(), items));
    }

    public synchronized MealPlan updateItem(UUID planId, UUID mealId, UUID itemId, String description) {
        MealPlan plan = requireDraft(planId);
        PlannedMeal meal = findMeal(plan, mealId);
        findItem(meal, itemId);
        List<MealItem> items = meal.items().stream()
                .map(item -> item.id().equals(itemId) ? new MealItem(itemId, requiredText(description)) : item)
                .toList();
        return replaceMeal(plan, new PlannedMeal(meal.id(), meal.name(), meal.time(), items));
    }

    public synchronized MealPlan removeItem(UUID planId, UUID mealId, UUID itemId) {
        MealPlan plan = requireDraft(planId);
        PlannedMeal meal = findMeal(plan, mealId);
        findItem(meal, itemId);
        List<MealItem> items = meal.items().stream().filter(item -> !item.id().equals(itemId)).toList();
        return replaceMeal(plan, new PlannedMeal(meal.id(), meal.name(), meal.time(), items));
    }

    public synchronized MealPlan activate(UUID planId) {
        MealPlan plan = requireDraft(planId);
        requireActiveFollowUp(plan.followUpId());
        if (plan.meals().size() < 3 || plan.meals().stream().anyMatch(meal -> meal.items().isEmpty())) {
            throw new InvalidRequestException("O plano precisa de três refeições, cada uma com pelo menos um item.");
        }
        repository.findByFollowUpId(plan.followUpId()).stream()
                .filter(MealPlan::active)
                .forEach(current -> repository.save(new MealPlan(current.id(), current.followUpId(), false,
                        current.meals())));
        return repository.save(new MealPlan(plan.id(), plan.followUpId(), true, plan.meals()));
    }

    public synchronized void deleteDraft(UUID planId) {
        repository.deleteById(requireDraft(planId).id());
    }

    private MealPlan requireDraft(UUID planId) {
        MealPlan plan = find(planId);
        if (plan.active()) {
            throw new InvalidRequestException("Um plano ativo não pode ser editado ou excluído.");
        }
        requireActiveFollowUp(plan.followUpId());
        return plan;
    }

    private void requireActiveFollowUp(UUID followUpId) {
        if (followUps.find(followUpId).status() != FollowUpStatus.ATIVO) {
            throw new InvalidRequestException("É necessário um acompanhamento ativo.");
        }
    }

    private PlannedMeal findMeal(MealPlan plan, UUID mealId) {
        return plan.meals().stream().filter(meal -> meal.id().equals(mealId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Refeição não encontrada no plano."));
    }

    private MealItem findItem(PlannedMeal meal, UUID itemId) {
        return meal.items().stream().filter(item -> item.id().equals(itemId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado na refeição."));
    }

    private MealPlan replaceMeal(MealPlan plan, PlannedMeal replacement) {
        return saveMeals(plan, plan.meals().stream()
                .map(meal -> meal.id().equals(replacement.id()) ? replacement : meal).toList());
    }

    private MealPlan saveMeals(MealPlan plan, List<PlannedMeal> meals) {
        return repository.save(new MealPlan(plan.id(), plan.followUpId(), false, meals));
    }

    private String requiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException("O texto não pode estar vazio.");
        }
        return value.trim();
    }
}
