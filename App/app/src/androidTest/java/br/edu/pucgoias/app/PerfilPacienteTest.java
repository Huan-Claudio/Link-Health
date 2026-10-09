package br.edu.pucgoias.app;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.paciente.HomePacienteActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class PerfilPacienteTest {
    @Test public void salvarContatoAtualizaPerfilEMantemCamposAoReabrir() {
        Sessao.entrar(Perfil.PACIENTE);
        try (ActivityScenario<HomePacienteActivity> scenario = ActivityScenario.launch(HomePacienteActivity.class)) {
            onView(withId(R.id.menuPerfil)).perform(scrollTo(), click());
            onView(withId(R.id.opcaoDados)).perform(scrollTo(), click());
            onView(withId(R.id.etNome)).perform(scrollTo(), replaceText("Ana Teste"));
            onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText("ana@exemplo.com"));
            onView(withId(R.id.etTelefone)).perform(scrollTo(), replaceText("62999998888"), closeSoftKeyboard());
            onView(withId(R.id.btnSalvar)).perform(scrollTo(), click());
            onView(withId(R.id.tvOlaNome)).check(matches(withSubstring("Ana Teste")));
            onView(withId(R.id.opcaoDados)).perform(scrollTo(), click());
            onView(withId(R.id.etNome)).check(matches(withText("Ana Teste")));
            onView(withId(R.id.etEmail)).check(matches(withText("ana@exemplo.com")));
            onView(withId(R.id.etTelefone)).check(matches(withText("62999998888")));
            onView(withId(R.id.etEmail)).perform(scrollTo(), replaceText("invalido"), closeSoftKeyboard());
            onView(withId(R.id.btnSalvar)).perform(scrollTo(), click());
            onView(withId(R.id.etEmail)).check(matches(hasErrorText("Informe um e-mail válido.")));
        }
    }

    @Test public void evolucaoAbreEVoltaParaHome() {
        Sessao.entrar(Perfil.PACIENTE);
        try (ActivityScenario<HomePacienteActivity> scenario = ActivityScenario.launch(HomePacienteActivity.class)) {
            onView(withId(R.id.menuEvolucao)).perform(scrollTo(), click());
            onView(withId(R.id.tvTituloHeader)).check(matches(withText(R.string.evolucao_corporal)));
            onView(withId(R.id.btnTirarFoto)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.btnVoltar)).perform(click());
            onView(withId(R.id.menuEvolucao)).check(matches(isDisplayed()));
        }
    }
}
