package br.edu.pucgoias.linkhealth;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasSize;
import br.edu.pucgoias.linkhealth.config.MobileSessionService;
import br.edu.pucgoias.linkhealth.domain.*;
import br.edu.pucgoias.linkhealth.service.*;
import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalTime;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Mesmo contrato executado em memória e no PostgreSQL real. */
abstract class RecordsMobileContract {
    static final String KEY = "link-health-test-api-key-12345678901234567890";
    @Autowired MockMvc mvc;
    @Autowired UserAccountService accounts;
    @Autowired FollowUpService followUps;
    @Autowired MealPlanService plans;
    @Autowired MobileSessionService sessions;
    UserAccount patient, other, nutritionist;
    String pt, ot, nt, base;
    FollowUp follow;
    @BeforeEach void fixtures() {
        patient = account(AccountRole.PACIENTE); other = account(AccountRole.PACIENTE); nutritionist = account(AccountRole.NUTRICIONISTA);
        pt = sessions.create(patient).token(); ot = sessions.create(other).token(); nt = sessions.create(nutritionist).token();
        base = "/api/v1/patients/" + patient.id();
        follow = followUps.invite(nutritionist.id(), patient.id(), "Objetivo real", 2500);
        followUps.respond(follow.id(), patient.id(), true);
    }
    private UserAccount account(AccountRole role) {
        String key = UUID.randomUUID().toString();
        return accounts.register("Pessoa teste", null, key + "@example.org", null, key.substring(0, 20), "senha1234", role);
    }
    ResultActions getAs(String path, String token) throws Exception { return mvc.perform(get(path).header("Authorization", "Bearer " + token)); }
    ResultActions postAs(String path, String body, String token) throws Exception { return mvc.perform(post(path).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body)); }
    String created(String route, String body) throws Exception {
        return JsonPath.read(postAs(base + route, body, pt).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }
    @Test void mobileAccessRequiresOwnerOrActiveNutritionistAndNeverAllowsNutritionistToWriteRecords() throws Exception {
        for (String resource : new String[]{"water-intakes", "meal-records", "weight-records", "evolution-photos", "records-summary", "records-history", "planned-meals"}) {
            mvc.perform(get(base + "/" + resource)).andExpect(status().isUnauthorized());
            getAs(base + "/" + resource, ot).andExpect(status().isForbidden());
            getAs(base + "/" + resource, nt).andExpect(status().isOk());
        }
        String[] routes = {"/water-intakes", "/meal-records", "/weight-records", "/evolution-photos"};
        String[] bodies = {"{\"amountMilliliters\":250}", "{\"mealName\":\"Almoço\"}", "{\"weightKg\":72.5}", "{\"originalFileName\":\"x.png\",\"contentType\":\"image/png\"}"};
        for (int i = 0; i < routes.length; i++) {
            postAs(base + routes[i], bodies[i], nt).andExpect(status().isForbidden());
            postAs(base + routes[i], bodies[i], ot).andExpect(status().isForbidden());
            String id = created(routes[i], bodies[i]);
            mvc.perform(delete(base + routes[i] + "/" + id).header("Authorization", "Bearer " + nt)).andExpect(status().isForbidden());
            mvc.perform(delete("/api/v1/patients/" + other.id() + routes[i] + "/" + id).header("Authorization", "Bearer " + ot)).andExpect(status().isNotFound());
            mvc.perform(delete(base + routes[i] + "/" + id).header("Authorization", "Bearer " + pt)).andExpect(status().isNoContent());
        }
        followUps.deactivate(follow.id(), nutritionist.id());
        getAs(base + "/records-history", nt).andExpect(status().isForbidden());
    }
    @Test void summaryAndHistoryUseActualRecordsAndRequestedTimezone() throws Exception {
        String at = "2020-01-02T01:00:00Z";
        created("/water-intakes", "{\"amountMilliliters\":350,\"consumedAt\":\"" + at + "\"}");
        created("/meal-records", "{\"mealName\":\"Jantar\",\"consumedAt\":\"" + at + "\"}");
        created("/weight-records", "{\"weightKg\":71.25,\"measuredAt\":\"" + at + "\"}");
        getAs(base + "/records-summary?date=2020-01-01&zoneId=America/Sao_Paulo", pt)
                .andExpect(status().isOk()).andExpect(jsonPath("$.waterMilliliters").value(350))
                .andExpect(jsonPath("$.completedMeals").value(1)).andExpect(jsonPath("$.latestWeightKg").value(71.25))
                .andExpect(jsonPath("$.waterGoalMilliliters").value(2500)).andExpect(jsonPath("$.objective").value("Objetivo real"));
        getAs(base + "/records-summary?date=2020-01-01&zoneId=UTC", pt).andExpect(status().isOk()).andExpect(jsonPath("$.waterMilliliters").value(0));
        getAs(base + "/records-history?zoneId=America/Sao_Paulo", pt).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].date").value("2020-01-01"));
        getAs(base + "/records-summary?zoneId=Invalid/Zone", pt).andExpect(status().isBadRequest());
    }
    @Test void waterAdjustmentIsAllOrNothingAndCannotMakeConsumptionNegative() throws Exception {
        String body = "{\"amountMilliliters\":500,\"date\":\"2020-01-01\",\"zoneId\":\"UTC\"}";
        postAs(base + "/water-intakes/adjustment", body, pt).andExpect(status().isOk());
        postAs(base + "/water-intakes/adjustment", body, pt).andExpect(status().isOk());
        postAs(base + "/water-intakes/adjustment", body.replace(":500", ":-750"), pt).andExpect(status().isOk());
        postAs(base + "/water-intakes/adjustment", body.replace(":500", ":-500"), pt).andExpect(status().isBadRequest());
        getAs(base + "/records-summary?date=2020-01-01&zoneId=UTC", pt).andExpect(status().isOk()).andExpect(jsonPath("$.waterMilliliters").value(250));
        postAs(base + "/water-intakes/adjustment", body.replace(":500", ":0"), pt).andExpect(status().isBadRequest());
    }
    @Test void plannedMealsCanBeCompletedOncePerDayAndUnmarkedWithoutAffectingThePlan() throws Exception {
        MealPlan plan = plans.createDraft(follow.id());
        for (int i = 0; i < 3; i++) {
            plan = plans.addMeal(plan.id(), "Refeição " + i, LocalTime.of(8 + i * 4, 0));
            plans.addItem(plan.id(), plan.meals().get(i).id(), "Alimento");
        }
        plan = plans.activate(plan.id());
        UUID meal = plan.meals().get(0).id();
        getAs(base + "/planned-meals", pt).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        String body = "{\"mealPlanItemId\":\"" + meal + "\",\"mealName\":\"Refeição realizada\"}";
        String id = created("/meal-records", body);
        postAs(base + "/meal-records", body, pt).andExpect(status().isBadRequest());
        postAs(base + "/meal-records", body.replace(meal.toString(), UUID.randomUUID().toString()), pt).andExpect(status().isBadRequest());
        mvc.perform(delete(base + "/meal-records/" + id).header("Authorization", "Bearer " + pt)).andExpect(status().isNoContent());
        created("/meal-records", body);
        getAs(base + "/planned-meals", pt).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
    }
    @Test void photoContentIsAuthenticatedAndPendingPhotosDoNotCountAsUploaded() throws Exception {
        String id = created("/evolution-photos", "{\"originalFileName\":\"imagem.png\",\"contentType\":\"image/png\"}");
        getAs(base + "/records-summary", pt).andExpect(status().isOk()).andExpect(jsonPath("$.photosCount").value(0));
        ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        byte[] png = out.toByteArray();
        mvc.perform(multipart(base + "/evolution-photos/" + id + "/content").file(new MockMultipartFile("file", "imagem.png", "image/png", png))
                .header("Authorization", "Bearer " + pt)).andExpect(status().isOk());
        getAs(base + "/evolution-photos/" + id + "/content", ot).andExpect(status().isForbidden());
        getAs(base + "/evolution-photos/" + id + "/content", nt).andExpect(status().isOk()).andExpect(content().bytes(png));
        getAs(base + "/records-summary", pt).andExpect(status().isOk()).andExpect(jsonPath("$.photosCount").value(1));
        mvc.perform(delete(base + "/evolution-photos/" + id).header("Authorization", "Bearer " + pt)).andExpect(status().isNoContent());
        getAs(base + "/evolution-photos/" + id + "/content", pt).andExpect(status().isNotFound());
    }
    @Test void validationRejectsUnknownPatientOverflowBlankNamesAndFutureDates() throws Exception {
        postAs(base + "/weight-records", "{\"weightKg\":72.001}", pt).andExpect(status().isBadRequest());
        String id = created("/meal-records", "{\"mealName\":\"Almoço\"}");
        mvc.perform(patch(base + "/meal-records/" + id).header("Authorization", "Bearer " + pt).contentType(MediaType.APPLICATION_JSON).content("{\"mealName\":\"   \"}"))
                .andExpect(status().isBadRequest());
        postAs(base + "/water-intakes", "{\"amountMilliliters\":500,\"consumedAt\":\"2999-01-01T00:00:00Z\"}", pt).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/patients/" + UUID.randomUUID() + "/water-intakes").header("X-API-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON).content("{\"amountMilliliters\":500}")).andExpect(status().isNotFound());
    }
}
