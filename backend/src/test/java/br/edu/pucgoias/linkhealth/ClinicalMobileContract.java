package br.edu.pucgoias.linkhealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "link-health.security.api-key=clinical-mobile-test-key-12345678901234567890",
        "link-health.photos.encryption-key=AQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQE=",
        "link-health.security.rate-limit-per-minute=10000"
})
@AutoConfigureMockMvc
abstract class ClinicalMobileContract {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    record Account(String id, String token, String email) {}

    Account account(String role) throws Exception {
        String unique = UUID.randomUUID().toString();
        String email = unique + "@example.org";
        JsonNode a = call("POST", "/api/v1/auth/register", null, Map.of("fullName", "Pessoa " + unique,
                "email", email, "password", "SenhaTeste123", "document", unique.substring(0, 28), "role", role), 201);
        JsonNode session = call("POST", "/api/v1/auth/mobile-session", null, Map.of("email", email, "password", "SenhaTeste123"), 200);
        return new Account(a.path("id").asText(), session.path("accessToken").asText(), email);
    }
    String invite(Account n, Account p) throws Exception {
        return call("POST", "/api/v1/follow-ups", n.token(), Map.of("nutritionistId", n.id(), "patientId", p.id()), 201).path("id").asText();
    }
    void respond(Account p, String id, boolean accept, int expected) throws Exception {
        call("POST", "/api/v1/follow-ups/" + id + "/response", p.token(), Map.of("patientId", p.id(), "accept", accept), expected);
    }
    @Test void invitationLifecycleUsesRealMobileAccountsAndRejectsImpersonation() throws Exception {
        Account n = account("NUTRICIONISTA"), p = account("PACIENTE"), other = account("PACIENTE"), n2 = account("NUTRICIONISTA");
        assertEquals(p.id(), call("GET", "/api/v1/patients/search?query=" + p.email(), n.token(), null, 200).get(0).path("id").asText());
        call("GET", "/api/v1/patients/search?query=Pessoa", p.token(), null, 403);
        call("POST", "/api/v1/follow-ups", n2.token(), Map.of("nutritionistId", n.id(), "patientId", p.id()), 403);
        String link = invite(n, p);
        call("POST", "/api/v1/follow-ups", n.token(), Map.of("nutritionistId", n.id(), "patientId", p.id()), 400);
        assertEquals("PENDENTE", call("GET", "/api/v1/me/follow-ups", p.token(), null, 200).get(0).path("status").asText());
        call("GET", "/api/v1/patients/" + p.id() + "/follow-ups", other.token(), null, 403);
        call("GET", "/api/v1/patients/" + p.id() + "/records-summary", n.token(), null, 403);
        call("POST", "/api/v1/follow-ups/" + link + "/response", other.token(), Map.of("patientId", p.id(), "accept", true), 403);
        respond(p, link, false, 200);
        respond(p, link, true, 400);
        link = invite(n, p);
        respond(p, link, true, 200);
        call("GET", "/api/v1/patients/" + p.id() + "/records-summary", n.token(), null, 200);
        call("POST", "/api/v1/follow-ups/" + link + "/deactivate", n2.token(), Map.of("nutritionistId", n.id()), 403);
        call("POST", "/api/v1/follow-ups/" + link + "/deactivate", n.token(), Map.of("nutritionistId", n.id()), 200);
        call("GET", "/api/v1/patients/" + p.id() + "/records-summary", n.token(), null, 403);
        assertNotNull(invite(n, p));
    }

    @Test void nutritionistPublishesPlanAndPatientUsesExistingMealRecordsWithoutAdminKey() throws Exception {
        Account n = account("NUTRICIONISTA"), p = account("PACIENTE"), n2 = account("NUTRICIONISTA");
        String link = invite(n, p), plans = "/api/v1/follow-ups/" + link + "/meal-plans";
        call("POST", plans, n.token(), null, 403);
        respond(p, link, true, 200);
        call("POST", plans, p.token(), null, 403);
        call("POST", plans, n2.token(), null, 403);
        String id = call("POST", plans, n.token(), null, 201).path("id").asText(), path = "/api/v1/meal-plans/" + id;
        call("GET", path, p.token(), null, 403);
        call("POST", path + "/activate", n.token(), null, 400);
        String firstMeal = null;
        for (int i = 0; i < 3; i++) {
            JsonNode plan = call("POST", path + "/meals", n.token(), Map.of("name", "Refeicao " + i, "time", "08:00:00"), 200);
            String meal = plan.path("meals").get(i).path("id").asText();
            if (firstMeal == null) firstMeal = meal;
            call("POST", path + "/meals/" + meal + "/items", n.token(), Map.of("description", "Fruta"), 200);
        }
        call("PUT", path + "/meals/" + firstMeal, n.token(), Map.of("name", "Cafe", "time", "07:30:00"), 200);
        call("POST", path + "/activate", n2.token(), null, 403);
        assertTrue(call("POST", path + "/activate", n.token(), null, 200).path("published").asBoolean());
        assertEquals(3, call("GET", "/api/v1/patients/" + p.id() + "/planned-meals", p.token(), null, 200).size());
        call("POST", "/api/v1/patients/" + p.id() + "/meal-records", p.token(), Map.of("mealPlanItemId", firstMeal, "mealName", "Cafe"), 201);
        call("DELETE", path, p.token(), null, 403);
        call("DELETE", path, n.token(), null, 400);
        JsonNode copy = call("POST", path + "/copy", n.token(), null, 201);
        assertFalse(copy.path("published").asBoolean());
        assertNotEquals(firstMeal, copy.path("meals").get(0).path("id").asText());
        call("POST", "/api/v1/meal-plans/" + copy.path("id").asText() + "/activate", n.token(), null, 200);
        assertFalse(call("GET", path, n.token(), null, 200).path("active").asBoolean());
        call("DELETE", path, n.token(), null, 400);
        call("POST", path + "/meals", n.token(), Map.of("name", "Mudanca", "time", "10:00:00"), 400);
        call("POST", "/api/v1/follow-ups/" + link + "/deactivate", n.token(), Map.of("nutritionistId", n.id()), 200);
        call("GET", path, n.token(), null, 403);
    }

    @Test void profileUpdatesPersistWithoutChangingRoleAndCredentialChangesRevokeSessions() throws Exception {
        Account p = account("PACIENTE"), n = account("NUTRICIONISTA"), other = account("PACIENTE");
        String link = invite(n, p);
        respond(p, link, true, 200);
        String originalDocument = call("GET", "/api/v1/me", p.token(), null, 200).path("document").asText();
        call("PATCH", "/api/v1/me", p.token(), Map.of("fullName", "Nome alterado", "email", p.email(), "phone", "62999990000", "birthDate", "2000-01-01"), 200);
        JsonNode updated = call("GET", "/api/v1/me", p.token(), null, 200);
        assertEquals("Nome alterado", updated.path("fullName").asText());
        assertEquals(originalDocument, updated.path("document").asText());
        assertEquals("PACIENTE", updated.path("role").asText());
        assertEquals("ATIVO", call("GET", "/api/v1/me/follow-ups", p.token(), null, 200).get(0).path("status").asText());
        call("PATCH", "/api/v1/me", p.token(), Map.of("fullName", "Nome alterado", "email", other.email(), "currentPassword", "SenhaTeste123"), 400);
        call("PATCH", "/api/v1/me", p.token(), Map.of("fullName", "Nome alterado", "email", p.email(), "newPassword", "NovaSenha123", "currentPassword", "Incorreta123"), 400);
        call("PATCH", "/api/v1/me", p.token(), Map.of("fullName", "Nome alterado", "email", p.email(), "newPassword", "NovaSenha123", "currentPassword", "SenhaTeste123"), 200);
        call("GET", "/api/v1/me", p.token(), null, 401);
        call("POST", "/api/v1/auth/mobile-session", null, Map.of("email", p.email(), "password", "SenhaTeste123"), 400);
        String token = call("POST", "/api/v1/auth/mobile-session", null, Map.of("email", p.email(), "password", "NovaSenha123"), 200).path("accessToken").asText();
        assertEquals(p.id(), call("GET", "/api/v1/me", token, null, 200).path("id").asText());
        call("POST", "/api/v1/auth/logout", token, null, 204);
        call("GET", "/api/v1/me", token, null, 401);
    }

    JsonNode call(String method, String path, String token, Object body, int expected) throws Exception {
        var r = request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON);
        if (token != null) r.header("Authorization", "Bearer " + token);
        if (body != null) r.content(json.writeValueAsBytes(body));
        String value = mvc.perform(r).andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return value.isEmpty() ? json.nullNode() : json.readTree(value);
    }
}
