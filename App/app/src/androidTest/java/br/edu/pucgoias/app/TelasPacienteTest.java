package br.edu.pucgoias.app;

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
    @Test public void planoAtualizaResumoAoVoltar() {
        MockData.getPacienteLogado().getPlano().forEach(r -> r.setConcluida(false));
        try (ActivityScenario<HomePacienteActivity> scenario = ActivityScenario.launch(HomePacienteActivity.class)) {
            onView(withId(R.id.menuPlano)).perform(scrollTo(), click());
            onView(withText(MockData.getPacienteLogado().getPlano().get(0).getTitulo()))
                    .perform(scrollTo(), click());
            onView(withId(R.id.btnVoltar)).perform(scrollTo(), click());
            onView(withId(R.id.tvRefeicoesConcluidas)).check(matches(withText(
                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                            .getTargetContext().getString(R.string.refeicoes_concluidas, 1, 4))));
        }
    }

    @Test public void compraContinuaMarcadaAoReabrir() {
        MockData.getPacienteLogado().getListaCompras().get(0).setComprado(false);
        String descricao = MockData.getPacienteLogado().getListaCompras().get(0).getDescricao();
        try (ActivityScenario<HomePacienteActivity> scenario = ActivityScenario.launch(HomePacienteActivity.class)) {
            onView(withId(R.id.menuLista)).perform(scrollTo(), click());
            onView(withText(descricao)).perform(scrollTo(), click());
            assertTrue(MockData.getPacienteLogado().getListaCompras().get(0).isComprado());
            onView(withId(R.id.btnVoltar)).perform(scrollTo(), click());
            onView(withId(R.id.menuLista)).perform(scrollTo(), click());
            assertTrue(MockData.getPacienteLogado().getListaCompras().get(0).isComprado());
            onView(withText(descricao)).perform(scrollTo(), click());
            assertFalse(MockData.getPacienteLogado().getListaCompras().get(0).isComprado());
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
