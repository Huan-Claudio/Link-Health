package br.edu.pucgoias.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import br.edu.pucgoias.app.data.remote.AccountSession;
import br.edu.pucgoias.app.ui.auth.LoginActivity;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class FluxoPacienteTest {
    @Test public void loginVazioMostraErros() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnEntrar)).perform(scrollTo(), click());
            onView(withId(R.id.etLogin)).check(matches(hasErrorText(
                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                            .getTargetContext().getString(R.string.email_invalido))));
            onView(withId(R.id.etSenha)).check(matches(hasErrorText("Campo obrigatório")));
        }
    }

    @Test public void loginAbreHomeComContaReal() throws Exception {
        AccountSession account = IntegrationTestSupport.patient();
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.etLogin)).perform(replaceText(account.getEmail()));
            onView(withId(R.id.etSenha)).perform(replaceText(IntegrationTestSupport.PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnEntrar)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitView(R.id.cardObjetivo);
            onView(withId(R.id.cardObjetivo)).check(matches(isDisplayed()));
        }
    }

    @Test public void cadastroAbreHome() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnCriarConta)).perform(scrollTo(), click());
            onView(withId(R.id.etNome)).perform(scrollTo(), replaceText("Paciente Teste"));
            onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText(IntegrationTestSupport.email()));
            onView(withId(R.id.etDocumento)).perform(scrollTo(), replaceText(IntegrationTestSupport.document()));
            onView(withId(R.id.etSenha)).perform(scrollTo(), replaceText(IntegrationTestSupport.PASSWORD), closeSoftKeyboard());
            onView(withId(R.id.btnFinalizar)).perform(scrollTo(), click());
            IntegrationTestSupport.awaitView(R.id.cardObjetivo);
            onView(withId(R.id.cardObjetivo)).check(matches(isDisplayed()));
        }
    }
}
