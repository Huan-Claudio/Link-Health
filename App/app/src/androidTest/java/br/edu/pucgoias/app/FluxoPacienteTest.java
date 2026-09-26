package br.edu.pucgoias.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import br.edu.pucgoias.app.data.MockData;
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
            onView(withId(R.id.etLogin)).check(matches(hasErrorText("Campo obrigatório")));
            onView(withId(R.id.etSenha)).check(matches(hasErrorText("Campo obrigatório")));
        }
    }

    @Test public void loginAbreHomeEAguaNaoFicaNegativa() {
        MockData.getPacienteLogado().setAguaConsumida(0);
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.etLogin)).perform(replaceText("paciente@exemplo.com"));
            onView(withId(R.id.etSenha)).perform(replaceText("teste123"), closeSoftKeyboard());
            onView(withId(R.id.btnEntrar)).perform(scrollTo(), click());
            onView(withId(R.id.btnMenos1L)).perform(scrollTo(), click());
            onView(withId(R.id.tvAgua)).check(matches(withText("0L de 5L")));
            onView(withId(R.id.btnMais500)).perform(scrollTo(), click());
            onView(withId(R.id.tvAgua)).check(matches(withText("0,5L de 5L")));
            onView(withId(R.id.menuPlano)).perform(scrollTo(), click());
            onView(withId(R.id.tvTituloHeader)).check(matches(withText(R.string.meu_plano_alimentar)));
        }
    }

    @Test public void cadastroAbreHome() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnCriarConta)).perform(scrollTo(), click());
            onView(withId(R.id.etNome)).perform(scrollTo(), replaceText("Paciente Teste"));
            onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText("teste@exemplo.com"));
            onView(withId(R.id.etDocumento)).perform(scrollTo(), replaceText("12345678900"));
            onView(withId(R.id.etSenha)).perform(scrollTo(), replaceText("teste123"), closeSoftKeyboard());
            onView(withId(R.id.btnFinalizar)).perform(scrollTo(), click());
            onView(withId(R.id.cardObjetivo)).check(matches(isDisplayed()));
        }
    }
}
