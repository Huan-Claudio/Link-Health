package br.edu.pucgoias.linkhealth;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.MealItem;
import br.edu.pucgoias.linkhealth.domain.MealPlan;
import br.edu.pucgoias.linkhealth.domain.MealPlanRepository;
import br.edu.pucgoias.linkhealth.domain.PlannedMeal;
import br.edu.pucgoias.linkhealth.service.FollowUpService;
import br.edu.pucgoias.linkhealth.service.MealPlanService;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.datasource.url=${LINK_HEALTH_TEST_DB_URL}",
        "spring.datasource.username=${LINK_HEALTH_TEST_DB_USERNAME}",
        "spring.datasource.password=${LINK_HEALTH_TEST_DB_PASSWORD:}",
        "link-health.security.api-key=link-health-test-api-key-12345678901234567890",
        "link-health.security.allowed-origins=http://localhost:3000",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE="
})
@ActiveProfiles("postgres")
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "LINK_HEALTH_TEST_DB_URL", matches = ".+")
class PostgresMealPlanPersistenceTest {
    private static final String API_KEY = "link-health-test-api-key-12345678901234567890";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MealPlanService plans;

    @Autowired
    private MealPlanRepository repository;

    @Autowired
    private UserAccountService accounts;

    @Autowired
    private FollowUpService followUps;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void persistsHttpEditingOrderRemovalAndCascadeDeletionOfDrafts() throws Exception {
        UUID followUpId = activeFollowUp();
        String created = mockMvc.perform(post("/api/v1/follow-ups/{id}/meal-plans", followUpId)
                        .header("X-API-Key", API_KEY))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID planId = UUID.fromString(objectMapper.readTree(created).get("id").asText());
        UUID firstMeal = addMeal(planId, "Café", 8);
        UUID secondMeal = addMeal(planId, "Almoço", 12);
        plans.addItem(planId, firstMeal, "Pão");
        plans.addItem(planId, firstMeal, "Fruta");
        UUID itemId = plans.find(planId).meals().get(0).items().get(1).id();
        mockMvc.perform(put("/api/v1/meal-plans/{planId}/meals/{mealId}", planId, firstMeal)
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Café da manhã", "time", "07:30:00"))))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/meal-plans/{planId}/meals/{mealId}/items/{itemId}", planId, firstMeal, itemId)
                        .header("X-API-Key", API_KEY).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "Banana"))))
                .andExpect(status().isOk());
        MealPlan reloaded = repository.findById(planId).orElseThrow();
        assertEquals(List.of(firstMeal, secondMeal), reloaded.meals().stream().map(PlannedMeal::id).toList());
        assertEquals("Banana", reloaded.meals().get(0).items().get(1).description());
        assertEquals(LocalTime.of(7, 30), reloaded.meals().get(0).time());
        UUID removedItem = reloaded.meals().get(0).items().get(0).id();
        plans.removeItem(planId, firstMeal, removedItem);
        assertEquals(itemId, repository.findById(planId).orElseThrow().meals().get(0).items().get(0).id());
        assertEquals(0, count("meal_plan_items", removedItem));
        plans.removeMeal(planId, firstMeal);
        assertEquals(secondMeal, repository.findById(planId).orElseThrow().meals().get(0).id());
        assertEquals(0, count("meal_plan_items", itemId));
        UUID remainingItem = plans.addItem(planId, secondMeal, "Arroz").meals().get(0).items().get(0).id();
        mockMvc.perform(delete("/api/v1/meal-plans/{id}", planId).header("X-API-Key", API_KEY))
                .andExpect(status().isNoContent());
        assertEquals(0, count("meal_plans", planId));
        assertEquals(0, count("planned_meals", secondMeal));
        assertEquals(0, count("meal_plan_items", remainingItem));
    }

    @Test
    void activationIsAtomicAndKeepsOneActivePlanWithValidMeals() throws Exception {
        UUID followUpId = activeFollowUp();
        MealPlan first = plans.createDraft(followUpId);
        assertThrows(InvalidRequestException.class, () -> plans.activate(first.id()));
        fill(first.id());
        mockMvc.perform(post("/api/v1/meal-plans/{id}/activate", first.id()).header("X-API-Key", API_KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
        assertThrows(InvalidRequestException.class, () -> plans.deleteDraft(first.id()));
        assertThrows(InvalidRequestException.class, () -> plans.addMeal(first.id(), "Lanche", LocalTime.NOON));
        MealPlan second = plans.createDraft(followUpId);
        fill(second.id());
        PlannedMeal invalidMeal = new PlannedMeal(UUID.randomUUID(), "Café", LocalTime.of(8, 0),
                List.of(new MealItem(UUID.randomUUID(), " ")));
        assertThrows(InvalidRequestException.class, () -> repository.activate(
                new MealPlan(second.id(), followUpId, true, List.of(invalidMeal))));
        assertTrue(repository.findById(first.id()).orElseThrow().active());
        assertFalse(repository.findById(second.id()).orElseThrow().active());
        assertEquals(3, repository.findById(second.id()).orElseThrow().meals().size());
        assertThrows(InvalidRequestException.class, () -> repository.save(new MealPlan(
                UUID.randomUUID(), followUpId, true, List.of())));
        plans.activate(second.id());
        assertFalse(repository.findById(first.id()).orElseThrow().active());
        assertEquals(1, repository.findByFollowUpId(followUpId).stream().filter(MealPlan::active).count());
        mockMvc.perform(get("/api/v1/meal-plans/{id}", second.id()).header("X-API-Key", API_KEY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meals.length()").value(3));
        assertThrows(InvalidRequestException.class, () -> repository.save(new MealPlan(
                UUID.randomUUID(), UUID.randomUUID(), false, List.of())));
    }

    @Test
    void allowsCorsPutRequestsForTheExistingMealEditingRoutes() throws Exception {
        mockMvc.perform(options("/api/v1/meal-plans/test/meals/test")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "Content-Type,X-API-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("PUT")));
    }

    private UUID activeFollowUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID nutritionistId = accounts.register("Ana", null, "ana-" + suffix + "@exemplo.com", null,
                "CRN-" + suffix, "senha1234", AccountRole.NUTRICIONISTA).id();
        UUID patientId = accounts.register("Maria", null, "maria-" + suffix + "@exemplo.com", null,
                suffix, "senha1234", AccountRole.PACIENTE).id();
        UUID id = followUps.invite(nutritionistId, patientId, null, 2000).id();
        return followUps.respond(id, patientId, true).id();
    }

    private UUID addMeal(UUID planId, String name, int hour) {
        MealPlan plan = plans.addMeal(planId, name, LocalTime.of(hour, 0));
        return plan.meals().get(plan.meals().size() - 1).id();
    }

    private void fill(UUID planId) {
        for (int hour : List.of(8, 12, 19)) {
            plans.addItem(planId, addMeal(planId, "Refeição " + hour, hour), "Alimento");
        }
    }

    private int count(String table, UUID id) {
        return jdbc.queryForObject("select count(*) from " + table + " where id = ?", Integer.class, id);
    }
}
