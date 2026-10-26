package br.edu.pucgoias.app;

import org.junit.Before;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.data.remote.AccountSession;
import org.json.JSONObject;
import static org.hamcrest.Matchers.containsString;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.ui.paciente.HomePacienteActivity;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TelasPacienteTest {

    @Before
    public void entrarComoPaciente() {
        Sessao.entrar(Perfil.PACIENTE);
    }

    @Test public void planoAtualizaResumoAoVoltar() throws Exception {
        AccountSession account = IntegrationTestSupport.patient();
        IntegrationTestSupport.enter(account);
        IntegrationTestSupport.http("POST", "/api/v1/patients/" + account.getId() + "/meal-records",
                new JSONObject().put("mealName", "Refeição real do teste"), account);
        try (ActivityScenario<HomePacienteActivity> scenario = ActivityScenario.launch(HomePacienteActivity.class)) {
            IntegrationTestSupport.awaitText("1 refeições registradas hoje");
            onView(withId(R.id.menuPlano)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitText("Refeição real do teste");
            onView(withId(R.id.btnVoltar)).perform(click());
            IntegrationTestSupport.awaitText("1 refeições registradas hoje");
            onView(withId(R.id.tvRefeicoesConcluidas)).check(matches(withSubstring("1 refeições registradas hoje")));
        }
    }

    @Test public void compraContinuaMarcadaAoReabrir() throws Exception {
        AccountSession account = IntegrationTestSupport.patient();
        IntegrationTestSupport.enter(account);
        String path = "/api/v1/patients/" + account.getId() + "/shopping-items";
        JSONObject body = new JSONObject().put("name", "CompraTesteAndroid").put("quantity", 1).put("unit", "UNIT");
        String created = IntegrationTestSupport.http("POST", path, body, account);
        String itemId = new JSONObject(created).getString("id");
        try (ActivityScenario<HomePacienteActivity> scenario = ActivityScenario.launch(HomePacienteActivity.class)) {
            onView(withId(R.id.menuLista)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitText("CompraTesteAndroid");
            onView(withText(containsString("CompraTesteAndroid"))).perform(click());
            IntegrationTestSupport.awaitPurchased(path, account, true);
            onView(withId(R.id.btnVoltar)).perform(click());
            onView(withId(R.id.menuLista)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitText("CompraTesteAndroid");
            IntegrationTestSupport.awaitPurchased(path, account, true);
            onView(withText(containsString("CompraTesteAndroid"))).perform(click());
            IntegrationTestSupport.awaitPurchased(path, account, false);
        } finally {
            IntegrationTestSupport.http("DELETE", path + "/" + itemId, null, account);
        }
    }

    @Test public void aceitarUltimoConviteMostraEstadoVazio() {
        MockData.getConvites().clear();
        MockData.getConvites().add("Nutricionista Teste");
        try (ActivityScenario<HomePacienteActivity> scenario = ActivityScenario.launch(HomePacienteActivity.class)) {
            onView(withId(R.id.menuConvites)).perform(scrollTo(), click());
            onView(withId(R.id.btnAdicionar)).perform(click());
            onView(withId(R.id.tvVazio)).check(matches(isDisplayed()));
            assertTrue(MockData.getConvites().isEmpty());
            onView(withId(R.id.btnVoltar)).perform(click());
            onView(withId(R.id.menuConvites)).perform(scrollTo(), click());
            onView(withId(R.id.tvVazio)).check(matches(isDisplayed()));
        }
    }
}
