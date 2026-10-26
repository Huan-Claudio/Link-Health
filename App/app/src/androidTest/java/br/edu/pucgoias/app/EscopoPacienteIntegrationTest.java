package br.edu.pucgoias.app;

import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import br.edu.pucgoias.app.data.remote.AccountSession;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import br.edu.pucgoias.app.ui.comum.EvolucaoCorporalActivity;
import br.edu.pucgoias.app.ui.nutricionista.RegistroDiarioActivity;
import br.edu.pucgoias.app.ui.nutricionista.EditarListaComprasActivity;
import br.edu.pucgoias.app.ui.paciente.HomePacienteActivity;
import br.edu.pucgoias.app.ui.paciente.PlanoAlimentarActivity;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

/** Fluxos da interface com a API real; execute contra banco exclusivo de testes. */
@RunWith(AndroidJUnit4.class)
public class EscopoPacienteIntegrationTest {
    private AccountSession patient() throws Exception {
        AccountSession account = IntegrationTestSupport.patient(); IntegrationTestSupport.enter(account); return account;
    }
    private String path(AccountSession a, String resource) { return "/api/v1/patients/" + a.getId() + "/" + resource; }
    private JSONArray list(AccountSession a, String resource) throws Exception { return new JSONArray(IntegrationTestSupport.http("GET", path(a, resource), null, a)); }
    private void count(AccountSession a, String resource, int count) throws Exception {
        long end = SystemClock.uptimeMillis() + 20_000;
        while (SystemClock.uptimeMillis() < end) { if (list(a, resource).length() == count) return; SystemClock.sleep(100); }
        fail("Contagem não persistida: " + resource);
    }
    private void saveDialog(String value) {
        onView(withId(R.id.recordInputValue)).perform(replaceText(value), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());
    }
    @Test public void aguaPersisteAposRotacaoEApareceNoHistorico() throws Exception {
        AccountSession a = patient();
        try (ActivityScenario<HomePacienteActivity> scenario = ActivityScenario.launch(HomePacienteActivity.class)) {
            IntegrationTestSupport.awaitText("0 ml consumidos");
            onView(withId(R.id.btnMais500)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitText("500 ml consumidos"); scenario.recreate();
            IntegrationTestSupport.awaitText("500 ml consumidos");
            onView(withId(R.id.btnMenos500)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitText("0 ml consumidos"); count(a, RecordsApi.WATER, 0);
        }
    }
    @Test public void refeicaoDaInterfacePodeSerCriadaEditadaERemovida() throws Exception {
        AccountSession a = patient();
        try (ActivityScenario<PlanoAlimentarActivity> scenario = ActivityScenario.launch(PlanoAlimentarActivity.class)) {
            IntegrationTestSupport.awaitText("Dados atualizados.");
            onView(withId(R.id.recordAddMeal)).perform(scrollTo(), click()); saveDialog("AlmocoUI");
            IntegrationTestSupport.awaitText("AlmocoUI"); count(a, RecordsApi.MEALS, 1); scenario.recreate();
            IntegrationTestSupport.awaitText("AlmocoUI");
            onView(withText("Editar refeição")).perform(scrollTo(), click()); saveDialog("JantarUI");
            IntegrationTestSupport.awaitText("JantarUI");
            assertEquals("JantarUI", list(a, RecordsApi.MEALS).getJSONObject(0).getString("mealName"));
            onView(withText("Remover refeição")).perform(scrollTo(), click());
            onView(withId(android.R.id.button1)).perform(click()); count(a, RecordsApi.MEALS, 0);
        }
    }
    @Test public void pesoDaInterfacePersisteEPermiteEdicaoRemocao() throws Exception {
        AccountSession a = patient();
        try (ActivityScenario<EvolucaoCorporalActivity> scenario = ActivityScenario.launch(EvolucaoCorporalActivity.class)) {
            IntegrationTestSupport.awaitText("Dados atualizados.");
            onView(withId(R.id.recordAddWeight)).perform(scrollTo(), click()); saveDialog("72.5");
            IntegrationTestSupport.awaitText("72.5 kg"); count(a, RecordsApi.WEIGHT, 1); scenario.recreate();
            IntegrationTestSupport.awaitText("72.5 kg");
            onView(withText("Editar peso")).perform(scrollTo(), click()); saveDialog("71.25");
            IntegrationTestSupport.awaitText("71.25 kg");
            assertEquals(71.25, list(a, RecordsApi.WEIGHT).getJSONObject(0).getDouble("weightKg"), 0.001);
            onView(withText("Remover peso")).perform(scrollTo(), click()); onView(withId(android.R.id.button1)).perform(click());
            count(a, RecordsApi.WEIGHT, 0);
        }
    }
    @Test public void historicoPermiteCorrigirERemoverAgua() throws Exception {
        AccountSession a = patient();
        IntegrationTestSupport.http("POST", path(a, RecordsApi.WATER), new JSONObject().put("amountMilliliters", 400), a);
        try (ActivityScenario<RegistroDiarioActivity> scenario = ActivityScenario.launch(RegistroDiarioActivity.class)) {
            IntegrationTestSupport.awaitText("400 ml de água");
            onView(withText("Editar registro")).perform(scrollTo(), click()); saveDialog("600");
            IntegrationTestSupport.awaitText("600 ml de água");
            onView(withText("Remover registro")).perform(scrollTo(), click()); onView(withId(android.R.id.button1)).perform(click());
            count(a, RecordsApi.WATER, 0); IntegrationTestSupport.awaitText("Nenhum registro encontrado.");
        }
    }
    @Test public void listaPermiteCriarEditarERemoverNaInterfaceSemDuplicarNaRotacao() throws Exception {
        AccountSession a = patient();
        try (ActivityScenario<EditarListaComprasActivity> scenario = ActivityScenario.launch(EditarListaComprasActivity.class)) {
            IntegrationTestSupport.awaitText("Adicionar item");
            onView(withId(R.id.btnRodape)).perform(click());
            onView(withId(R.id.etShoppingName)).perform(replaceText("AveiaUI"));
            onView(withId(R.id.etShoppingQuantity)).perform(replaceText("2.5"), closeSoftKeyboard()); scenario.recreate();
            onView(withId(R.id.etShoppingName)).check(matches(withText("AveiaUI")));
            onView(withId(R.id.btnShoppingSave)).perform(click()); count(a, "shopping-items", 1);
            IntegrationTestSupport.awaitText("A comprar");
            onView(withId(R.id.etShoppingName)).perform(replaceText("AveiaEditada"), closeSoftKeyboard());
            onView(withId(R.id.btnShoppingSave)).perform(click());
            long end = SystemClock.uptimeMillis() + 15_000;
            while (!"AveiaEditada".equals(list(a, "shopping-items").getJSONObject(0).getString("name")) && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
            assertEquals("AveiaEditada", list(a, "shopping-items").getJSONObject(0).getString("name"));
            onView(withId(R.id.btnShoppingDelete)).perform(click()); onView(withId(android.R.id.button1)).perform(click()); count(a, "shopping-items", 0);
        }
    }
    @Test public void transporteDeFotoUsaSessaoEImagemPodeSerVistaERemovidaNaTela() throws Exception {
        AccountSession a = patient(); RecordsApi api = new RecordsApi();
        String created = IntegrationTestSupport.http("POST", path(a, RecordsApi.PHOTOS), new JSONObject().put("originalFileName", "android.jpg").put("contentType", "image/jpeg"), a);
        String id = new JSONObject(created).getString("id");
        Bitmap bitmap = Bitmap.createBitmap(12, 12, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream out = new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); bitmap.recycle();
        CountDownLatch upload = new CountDownLatch(1); AtomicReference<String> error = new AtomicReference<>();
        api.upload(a.getId(), a.getAccessToken(), id, out.toByteArray(), new RecordsApi.Callback<JSONObject>() {
            public void onSuccess(JSONObject value) { upload.countDown(); }
            public void onError(String value) { error.set(value); upload.countDown(); }
        });
        assertTrue(upload.await(20, TimeUnit.SECONDS)); assertNull(error.get());
        try (ActivityScenario<EvolucaoCorporalActivity> scenario = ActivityScenario.launch(EvolucaoCorporalActivity.class)) {
            IntegrationTestSupport.awaitText("Foto armazenada");
            onView(withText("Ver foto")).perform(scrollTo(), click());
            IntegrationTestSupport.awaitDialogText("Foto de evolução");
            onView(withContentDescription("Foto de evolução")).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());
            onView(withText("Remover foto")).perform(scrollTo(), click()); onView(withId(android.R.id.button1)).perform(click()); count(a, RecordsApi.PHOTOS, 0);
        }
    }
    @Test public void envioPendentePngPodeSerRetomadoSemCriarOutraFoto() throws Exception {
        AccountSession a = patient();
        String response = IntegrationTestSupport.http("POST", path(a, RecordsApi.PHOTOS), new JSONObject()
                .put("originalFileName", "pendente.png").put("contentType", "image/png"), a);
        String id = new JSONObject(response).getString("id");
        android.content.Context context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext();
        java.io.File input = java.io.File.createTempFile("test-photo-", ".png", context.getCacheDir());
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(input)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); }
        bitmap.recycle();
        try (ActivityScenario<EvolucaoCorporalActivity> scenario = ActivityScenario.launch(EvolucaoCorporalActivity.class)) {
            IntegrationTestSupport.awaitText("Envio pendente");
            scenario.onActivity(activity -> new androidx.lifecycle.ViewModelProvider(activity)
                    .get(br.edu.pucgoias.app.ui.comum.RecordsViewModel.class).preparePhoto(
                            context.getContentResolver(), android.net.Uri.fromFile(input), id, null));
            IntegrationTestSupport.awaitText("Foto armazenada");
            JSONArray photos = list(a, RecordsApi.PHOTOS); assertEquals(1, photos.length());
            assertEquals(id, photos.getJSONObject(0).getString("id")); assertEquals("image/png", photos.getJSONObject(0).getString("contentType"));
        } finally { input.delete(); }
    }
}
