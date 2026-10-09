package br.edu.pucgoias.linkhealth.api.mealplan;

import br.edu.pucgoias.linkhealth.domain.MealPlan;
import br.edu.pucgoias.linkhealth.service.MealPlanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "postgres"})
@RequestMapping("/api/v1")
public class MealPlanController {
    private final MealPlanService service;

    public MealPlanController(MealPlanService service) {
        this.service = service;
    }

    @PostMapping("/follow-ups/{followUpId}/meal-plans")
    public ResponseEntity<MealPlan> create(@PathVariable UUID followUpId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDraft(followUpId));
    }

    @GetMapping("/follow-ups/{followUpId}/meal-plans")
    public List<MealPlan> list(@PathVariable UUID followUpId) {
        return service.byFollowUp(followUpId);
    }

    @GetMapping("/meal-plans/{planId}")
    public MealPlan find(@PathVariable UUID planId) {
        return service.find(planId);
    }

    @DeleteMapping("/meal-plans/{planId}")
    public ResponseEntity<Void> delete(@PathVariable UUID planId) {
        service.deleteDraft(planId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/meal-plans/{planId}/activate")
    public MealPlan activate(@PathVariable UUID planId) {
        return service.activate(planId);
    }

    @PostMapping("/meal-plans/{planId}/meals")
    public MealPlan addMeal(@PathVariable UUID planId, @Valid @RequestBody MealRequest request) {
        return service.addMeal(planId, request.name(), request.time());
    }

    @PutMapping("/meal-plans/{planId}/meals/{mealId}")
    public MealPlan updateMeal(@PathVariable UUID planId, @PathVariable UUID mealId,
            @Valid @RequestBody MealRequest request) {
        return service.updateMeal(planId, mealId, request.name(), request.time());
    }

    @DeleteMapping("/meal-plans/{planId}/meals/{mealId}")
    public MealPlan removeMeal(@PathVariable UUID planId, @PathVariable UUID mealId) {
        return service.removeMeal(planId, mealId);
    }

    @PostMapping("/meal-plans/{planId}/meals/{mealId}/items")
    public MealPlan addItem(@PathVariable UUID planId, @PathVariable UUID mealId,
            @Valid @RequestBody MealItemRequest request) {
        return service.addItem(planId, mealId, request.description());
    }

    @PutMapping("/meal-plans/{planId}/meals/{mealId}/items/{itemId}")
    public MealPlan updateItem(@PathVariable UUID planId, @PathVariable UUID mealId, @PathVariable UUID itemId,
            @Valid @RequestBody MealItemRequest request) {
        return service.updateItem(planId, mealId, itemId, request.description());
    }

    @DeleteMapping("/meal-plans/{planId}/meals/{mealId}/items/{itemId}")
    public MealPlan removeItem(@PathVariable UUID planId, @PathVariable UUID mealId, @PathVariable UUID itemId) {
        return service.removeItem(planId, mealId, itemId);
    }
}
