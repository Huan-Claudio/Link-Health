package br.edu.pucgoias.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import br.edu.pucgoias.app.data.remote.*;
import br.edu.pucgoias.app.ui.comum.*;
import br.edu.pucgoias.app.ui.nutricionista.*;
import br.edu.pucgoias.app.ui.paciente.*;

import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class EscopoPacienteIntegrationTest {
    private AccountSession patient() throws Exception {
        AccountSession a = IntegrationTestSupport.patient();
        IntegrationTestSupport.enter(a);
        return a;
    }

    private String path(AccountSession a, String r) {
        return "/api/v1/patients/" + a.getId() + "/" + r;
    }

    private JSONArray list(AccountSession a, String r) throws Exception {
        return new JSONArray(IntegrationTestSupport.http("GET", path(a, r), null, a));
    }

    private void count(AccountSession a, String r, int expected) throws Exception {
        long end = SystemClock.uptimeMillis() + 20000;
        while (SystemClock.uptimeMillis() < end) {
            if (list(a, r).length() == expected) return;
            SystemClock.sleep(100);
        }
        fail("Contagem não persistida: " + r);
    }

    private void saveDialog(String value) {
        onView(withId(R.id.recordInputValue)).perform(replaceText(value), closeSoftKeyboard());
        onView(withId(android.R.id.button1)).perform(click());
    }

    @Test
    public void aguaPersisteAposRotacaoECorrecaoDefineOTotal() throws Exception {
        AccountSession a = patient();
        try (ActivityScenario<HomePacienteActivity> s =
                ActivityScenario.launch(HomePacienteActivity.class)) {
            IntegrationTestSupport.awaitText("0 ml consumidos");
            onView(withId(R.id.btnMais500)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitText("500 ml consumidos");
            s.recreate();
            IntegrationTestSupport.awaitText("500 ml consumidos");
            onView(withId(R.id.btnPersonalizado)).perform(scrollTo(), click());
            onView(withText("Corrigir total de hoje")).perform(click());
            onView(withId(R.id.waterCustomAmount)).perform(replaceText("0"), closeSoftKeyboard());
            onView(withId(android.R.id.button1)).perform(click());
            IntegrationTestSupport.awaitText("0 ml consumidos");
            count(a, RecordsApi.WATER, 0);
        }
    }

    @Test
    public void refeicaoDaInterfacePodeSerCriadaEditadaERemovida() throws Exception {
        AccountSession a = patient();
        try (ActivityScenario<PlanoAlimentarActivity> s =
                ActivityScenario.launch(PlanoAlimentarActivity.class)) {
            IntegrationTestSupport.awaitEnabled(R.id.recordAddMeal);
            onView(withId(R.id.recordAddMeal)).perform(scrollTo(), click());
            saveDialog("AlmocoUI");
            IntegrationTestSupport.awaitText("AlmocoUI");
            count(a, RecordsApi.MEALS, 1);
            s.recreate();
            IntegrationTestSupport.awaitText("AlmocoUI");
            onView(withText("Editar refeição")).perform(scrollTo(), click());
            saveDialog("JantarUI");
            IntegrationTestSupport.awaitText("JantarUI");
            assertEquals(
                    "JantarUI", list(a, RecordsApi.MEALS).getJSONObject(0).getString("mealName"));
            onView(withText("Remover refeição")).perform(scrollTo(), click());
            onView(withId(android.R.id.button1)).perform(click());
            count(a, RecordsApi.MEALS, 0);
        }
    }

    @Test
    public void historicoPermiteCorrigirERemoverAgua() throws Exception {
        AccountSession a = patient();
        IntegrationTestSupport.http(
                "POST",
                path(a, RecordsApi.WATER),
                new JSONObject().put("amountMilliliters", 400),
                a);
        try (ActivityScenario<RegistroDiarioActivity> s =
                ActivityScenario.launch(RegistroDiarioActivity.class)) {
            IntegrationTestSupport.awaitText("400 ml de água");
            onView(withText("Editar registro")).perform(scrollTo(), click());
            saveDialog("600");
            IntegrationTestSupport.awaitText("600 ml de água");
            onView(withText("Remover registro")).perform(scrollTo(), click());
            onView(withId(android.R.id.button1)).perform(click());
            count(a, RecordsApi.WATER, 0);
            IntegrationTestSupport.awaitText("Nenhum registro encontrado.");
        }
    }

    @Test
    public void listaPermiteCriarEditarERemoverNaInterfaceSemDuplicarNaRotacao() throws Exception {
        AccountSession a = patient();
        try (ActivityScenario<EditarListaComprasActivity> s =
                ActivityScenario.launch(EditarListaComprasActivity.class)) {
            IntegrationTestSupport.awaitText("Adicionar item");
            onView(withId(R.id.btnRodape)).perform(click());
            onView(withId(R.id.etShoppingName)).perform(replaceText("AveiaUI"));
            onView(withId(R.id.etShoppingQuantity))
                    .perform(replaceText("2.5"), closeSoftKeyboard());
            s.recreate();
            onView(withId(R.id.etShoppingName)).check(matches(withText("AveiaUI")));
            onView(withId(R.id.btnShoppingSave)).perform(click());
            count(a, "shopping-items", 1);
            IntegrationTestSupport.awaitText("A comprar");
            onView(withId(R.id.etShoppingName))
                    .perform(replaceText("AveiaEditada"), closeSoftKeyboard());
            onView(withId(R.id.btnShoppingSave)).perform(click());
            long end = SystemClock.uptimeMillis() + 15000;
            while (!"AveiaEditada"
                            .equals(list(a, "shopping-items").getJSONObject(0).getString("name"))
                    && SystemClock.uptimeMillis() < end) SystemClock.sleep(100);
            assertEquals(
                    "AveiaEditada", list(a, "shopping-items").getJSONObject(0).getString("name"));
            onView(withId(R.id.btnShoppingDelete)).perform(click());
            onView(withId(android.R.id.button1)).perform(click());
            count(a, "shopping-items", 0);
        }
    }

    private File picture() throws Exception {
        File file =
                File.createTempFile(
                        "assessment-test-",
                        ".png",
                        InstrumentationRegistry.getInstrumentation()
                                .getTargetContext()
                                .getCacheDir());
        Bitmap b = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888);
        b.eraseColor(android.graphics.Color.GREEN);
        try (FileOutputStream out = new FileOutputStream(file)) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        b.recycle();
        return file;
    }

    private void addPhoto(ActivityScenario<NovaEvolucaoActivity> s, File file, int slot) {
        s.onActivity(
                a ->
                        new ViewModelProvider(a)
                                .get(AssessmentDraftViewModel.class)
                                .prepare(a.getContentResolver(), Uri.fromFile(file), slot, null));
        IntegrationTestSupport.awaitText((slot + 1) + " de 3 fotos adicionadas");
    }

    @Test
    public void tresFotosEPesoSaoSalvosJuntosEAlbumTemTresPaginas() throws Exception {
        AccountSession a = patient();
        File file = picture();
        try (ActivityScenario<NovaEvolucaoActivity> s =
                ActivityScenario.launch(NovaEvolucaoActivity.class)) {
            addPhoto(s, file, 0);
            addPhoto(s, file, 1);
            s.recreate();
            IntegrationTestSupport.awaitText("2 de 3 fotos adicionadas");
            addPhoto(s, file, 2);
            onView(withId(R.id.assessmentWeight))
                    .perform(scrollTo(), replaceText("72.5"), closeSoftKeyboard());
            onView(withId(R.id.assessmentSave)).perform(scrollTo(), click());
            count(a, RecordsApi.ASSESSMENTS, 1);
        } finally {
            file.delete();
        }
        assertEquals(3, list(a, RecordsApi.PHOTOS).length());
        assertEquals(1, list(a, RecordsApi.WEIGHT).length());
        try (ActivityScenario<EvolucaoCorporalActivity> s =
                ActivityScenario.launch(EvolucaoCorporalActivity.class)) {
            IntegrationTestSupport.awaitText("72.5 kg");
            onView(withText("Ver 3 fotos")).perform(scrollTo(), click());
            IntegrationTestSupport.awaitDialogText("Registro de evolução");
            onView(withText("Frente")).check(matches(isDisplayed()));
            onView(withContentDescription("Próxima foto")).perform(click());
            onView(withText("Lado")).check(matches(isDisplayed()));
            onView(withContentDescription("Próxima foto")).perform(click());
            onView(withText("Costas")).check(matches(isDisplayed()));
            onView(withId(android.R.id.button1)).perform(click());
            onView(withText("Remover avaliação")).perform(scrollTo(), click());
            onView(withId(android.R.id.button1)).perform(click());
            count(a, RecordsApi.ASSESSMENTS, 0);
            count(a, RecordsApi.PHOTOS, 0);
            count(a, RecordsApi.WEIGHT, 0);
        }
    }

    @Test
    public void avaliacaoIncompletaNaoCriaFotosOuPesoNoServidor() throws Exception {
        AccountSession a = patient();
        File file = picture();
        try (ActivityScenario<NovaEvolucaoActivity> s =
                ActivityScenario.launch(NovaEvolucaoActivity.class)) {
            addPhoto(s, file, 0);
            addPhoto(s, file, 1);
            s.recreate();
            IntegrationTestSupport.awaitText("2 de 3 fotos adicionadas");
            onView(withId(R.id.assessmentSave))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
            count(a, RecordsApi.ASSESSMENTS, 0);
            count(a, RecordsApi.PHOTOS, 0);
            count(a, RecordsApi.WEIGHT, 0);
        } finally {
            file.delete();
        }
    }

    @Test
    public void repetirEnvioCompletoUsaMesmoRegistro() throws Exception {
        AccountSession a = patient();
        File input = picture();
        byte[] jpeg;
        Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(input.getAbsolutePath());
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            jpeg = out.toByteArray();
        }
        bitmap.recycle();
        input.delete();
        JSONObject details =
                new JSONObject().put("requestId", UUID.randomUUID().toString()).put("weightKg", 70);
        for (int i = 0; i < 2; i++) {
            CountDownLatch done = new CountDownLatch(1);
            AtomicReference<String> error = new AtomicReference<>();
            new RecordsApi()
                    .assessment(
                            a.getId(),
                            a.getAccessToken(),
                            details,
                            new byte[][] {jpeg, jpeg, jpeg},
                            new RecordsApi.Callback<JSONObject>() {
                                public void onSuccess(JSONObject v) {
                                    done.countDown();
                                }

                                public void onError(String m) {
                                    error.set(m);
                                    done.countDown();
                                }
                            });
            assertTrue(done.await(25, TimeUnit.SECONDS));
            assertNull(error.get());
        }
        count(a, RecordsApi.ASSESSMENTS, 1);
        count(a, RecordsApi.PHOTOS, 3);
        count(a, RecordsApi.WEIGHT, 1);
    }
}
