package br.edu.pucgoias.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import android.os.Bundle;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import br.edu.pucgoias.app.data.remote.*;
import br.edu.pucgoias.app.ui.nutricionista.HomeNutricionistaActivity;
import br.edu.pucgoias.app.ui.paciente.PlanoAlimentarActivity;

import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Usa duas contas sintéticas vinculadas, preparadas fora do APK. Nunca recebe chave administrativa.
 */
@RunWith(AndroidJUnit4.class)
public class EscopoVinculadoIntegrationTest {
    private AccountSession patient, nutritionist;

    @Before
    public void prepare() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        Assume.assumeTrue(
                "Informe contas vinculadas de um banco exclusivo de testes.",
                args.containsKey("scopePatientEmail")
                        && args.containsKey("scopeNutritionistEmail"));
        patient = login(args.getString("scopePatientEmail"));
        nutritionist = login(args.getString("scopeNutritionistEmail"));
        IntegrationTestSupport.enter(nutritionist);
    }

    private AccountSession login(String email) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<AccountSession> value = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        new AccountApi()
                .login(
                        email,
                        "AuditOnly123",
                        new AccountApi.Callback() {
                            public void onSuccess(AccountSession session) {
                                value.set(session);
                                done.countDown();
                            }

                            public void onError(String message) {
                                error.set(message);
                                done.countDown();
                            }
                        });
        assertTrue(done.await(20, TimeUnit.SECONDS));
        assertNull(error.get());
        assertNotNull(value.get());
        return value.get();
    }

    private String path(String resource) {
        return "/api/v1/patients/" + patient.getId() + "/" + resource;
    }

    private JSONArray items(String resource) throws Exception {
        return new JSONArray(IntegrationTestSupport.http("GET", path(resource), null, patient));
    }

    private void count(String resource, int expected) throws Exception {
        long end = SystemClock.uptimeMillis() + 15_000;
        while (SystemClock.uptimeMillis() < end) {
            if (items(resource).length() == expected) return;
            SystemClock.sleep(100);
        }
        fail("Operação não persistida: " + resource);
    }

    private void ficha() {
        IntegrationTestSupport.awaitText(patient.getNome());
        onView(withText(patient.getNome())).perform(click());
        IntegrationTestSupport.awaitText("Objetivo de teste");
    }

    @Test
    public void nutricionistaEditaAListaRealPelaFicha() throws Exception {
        try (ActivityScenario<HomeNutricionistaActivity> scenario =
                ActivityScenario.launch(HomeNutricionistaActivity.class)) {
            ficha();
            onView(withId(R.id.menuEditarLista)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitText("Adicionar item");
            onView(withId(R.id.btnRodape)).perform(click());
            onView(withId(R.id.etShoppingName)).perform(replaceText("CompraNutriUI"));
            onView(withId(R.id.etShoppingQuantity)).perform(replaceText("3"), closeSoftKeyboard());
            onView(withId(R.id.btnShoppingSave)).perform(click());
            count("shopping-items", 1);
            IntegrationTestSupport.awaitText("A comprar");
            assertEquals(
                    "CompraNutriUI", items("shopping-items").getJSONObject(0).getString("name"));
            onView(withId(R.id.btnShoppingDelete)).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            count("shopping-items", 0);
        }
    }

    @Test
    public void produtosPodemSerCriadosEditadosERemovidosPelaFichaReal() throws Exception {
        try (ActivityScenario<HomeNutricionistaActivity> scenario =
                ActivityScenario.launch(HomeNutricionistaActivity.class)) {
            ficha();
            onView(withId(R.id.menuEditarProdutos)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitEnabled(R.id.btnRodape);
            onView(withId(R.id.btnRodape)).perform(click());
            onView(withId(R.id.etNomeProduto)).perform(replaceText("ProdutoNutriUI"));
            onView(withId(R.id.etPrecoProduto)).perform(replaceText("19.50"));
            onView(withId(R.id.etLinkProduto))
                    .perform(replaceText("https://example.org/produto"), closeSoftKeyboard());
            onView(withId(R.id.btnSalvarProduto)).perform(click());
            count("recommended-products", 1);
            IntegrationTestSupport.awaitEnabled(R.id.btnSalvarProduto);
            onView(withId(R.id.etNomeProduto))
                    .perform(replaceText("ProdutoAtualizadoUI"), closeSoftKeyboard());
            onView(withId(R.id.btnSalvarProduto)).perform(click());
            long end = SystemClock.uptimeMillis() + 15_000;
            while (!"ProdutoAtualizadoUI"
                            .equals(
                                    items("recommended-products")
                                            .getJSONObject(0)
                                            .getString("name"))
                    && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
            assertEquals(
                    "ProdutoAtualizadoUI",
                    items("recommended-products").getJSONObject(0).getString("name"));
            IntegrationTestSupport.awaitEnabled(R.id.btnSalvarProduto);
            onView(withId(R.id.btnRemover)).perform(click());
            count("recommended-products", 0);
        }
    }

    @Test
    public void refeicaoPlanejadaPodeSerMarcadaEDesmarcadaNaTela() throws Exception {
        IntegrationTestSupport.enter(patient);
        JSONArray planned = items("planned-meals");
        assertTrue("Prepare um plano ativo no banco de testes.", planned.length() > 0);
        String id = planned.getJSONObject(0).getString("id");
        try (ActivityScenario<PlanoAlimentarActivity> scenario =
                ActivityScenario.launch(PlanoAlimentarActivity.class)) {
            IntegrationTestSupport.awaitEnabled(R.id.recordAddMeal);
            onView(
                            allOf(
                                    withText("Marcar como realizada"),
                                    isDescendantOfA(withTagValue(is("planned-" + id)))))
                    .perform(scrollTo(), click());
            count("meal-records", 1);
            IntegrationTestSupport.awaitText("Desmarcar realização");
            scenario.recreate();
            IntegrationTestSupport.awaitText("Desmarcar realização");
            onView(
                            allOf(
                                    withText("Desmarcar realização"),
                                    isDescendantOfA(withTagValue(is("planned-" + id)))))
                    .perform(scrollTo(), click());
            count("meal-records", 0);
        }
    }

    @Test
    public void metaDefinidaNaFichaFazABarraDoPacienteCrescer() throws Exception {
        try {
            try (ActivityScenario<HomeNutricionistaActivity> s =
                    ActivityScenario.launch(HomeNutricionistaActivity.class)) {
                ficha();
                onView(withId(R.id.btnEditarMeta)).perform(scrollTo(), click());
                IntegrationTestSupport.awaitDialogText(
                        "Essas informações aparecerão na home do paciente.");
                onView(withId(R.id.careObjectiveInput)).perform(replaceText("Ganho de massa"));
                onView(withId(R.id.careGoalInput)).perform(replaceText("3"), closeSoftKeyboard());
                onView(withId(android.R.id.button1)).perform(click());
                IntegrationTestSupport.awaitText("Ganho de massa");
            }
            IntegrationTestSupport.enter(patient);
            try (ActivityScenario<br.edu.pucgoias.app.ui.paciente.HomePacienteActivity> s =
                    ActivityScenario.launch(
                            br.edu.pucgoias.app.ui.paciente.HomePacienteActivity.class)) {
                IntegrationTestSupport.awaitText("0 ml consumidos de 3000 ml");
                onView(withId(R.id.btnMais500)).perform(scrollTo(), click());
                IntegrationTestSupport.awaitText("500 ml consumidos de 3000 ml");
                s.onActivity(
                        a ->
                                assertEquals(
                                        17,
                                        ((com.google.android.material.progressindicator
                                                                .LinearProgressIndicator)
                                                        a.findViewById(R.id.progressAgua))
                                                .getProgress()));
            }
        } finally {
            IntegrationTestSupport.http(
                    "PATCH",
                    path("care-settings"),
                    new JSONObject().put("objective", "Objetivo de teste").put("waterGoalMl", 2500),
                    nutritionist);
            IntegrationTestSupport.http(
                    "POST",
                    path("water-intakes/daily-total"),
                    new JSONObject()
                            .put("amountMilliliters", 0)
                            .put(
                                    "date",
                                    java.time.LocalDate.now(
                                                    java.time.ZoneId.of("America/Sao_Paulo"))
                                            .toString())
                            .put("zoneId", "America/Sao_Paulo"),
                    patient);
        }
    }

    @Test
    public void produtoRecomendadoApareceNaListaDoPaciente() throws Exception {
        String response =
                IntegrationTestSupport.http(
                        "POST",
                        path("recommended-products"),
                        new JSONObject()
                                .put("name", "ProdutoVisivelUI")
                                .put("price", 19.5)
                                .put("purchaseUrl", "https://example.org/produto"),
                        nutritionist);
        String id = new JSONObject(response).getString("id");
        IntegrationTestSupport.enter(patient);
        try (ActivityScenario<br.edu.pucgoias.app.ui.paciente.ListaComprasActivity> s =
                ActivityScenario.launch(
                        br.edu.pucgoias.app.ui.paciente.ListaComprasActivity.class)) {
            IntegrationTestSupport.awaitText("ProdutoVisivelUI");
            onView(withText("ProdutoVisivelUI")).check(matches(isDisplayed()));
        } finally {
            IntegrationTestSupport.http(
                    "DELETE", path("recommended-products") + "/" + id, null, nutritionist);
        }
    }
}
