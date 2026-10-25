package br.edu.pucgoias.app;

import android.app.Activity;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.AccountApi;
import br.edu.pucgoias.app.data.remote.AccountSession;
import br.edu.pucgoias.app.model.Perfil;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;

/** Contas inéditas e espera explícita: testes reais não dependem dos dados dos integrantes. */
final class IntegrationTestSupport {
    static final String PASSWORD = "SenhaTeste123";
    private IntegrationTestSupport() { }

    static String email() { return UUID.randomUUID() + "@example.org"; }
    static String document() {
        String value = Long.toString(System.nanoTime());
        return value.substring(value.length() - 11);
    }

    static AccountSession patient() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<AccountSession> session = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        new AccountApi().register(new AccountApi.RegisterData("Paciente de Teste", "", email(), "",
                document(), PASSWORD, Perfil.PACIENTE), new AccountApi.Callback() {
            public void onSuccess(AccountSession value) { session.set(value); done.countDown(); }
            public void onError(String message) { error.set(message); done.countDown(); }
        });
        assertTrue("A API deve estar iniciada para os testes de integração.", done.await(25, TimeUnit.SECONDS));
        assertNull("Falha na API: " + error.get(), error.get());
        assertNotNull(session.get());
        return session.get();
    }

    static void enter(AccountSession session) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> Sessao.entrar(session));
    }

    static void awaitView(int id) {
        await(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                View view = activity.findViewById(id);
                if (view != null && view.isShown()) return true;
            }
            return false;
        });
    }

    static void awaitText(String text) {
        await(() -> {
            for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                if (containsText(activity.getWindow().getDecorView(), text)) return true;
            }
            return false;
        });
    }

    private interface Condition { boolean ready(); }
    private static void await(Condition condition) {
        long limit = SystemClock.uptimeMillis() + 25_000;
        AtomicBoolean ready = new AtomicBoolean();
        while (SystemClock.uptimeMillis() < limit) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> ready.set(condition.ready()));
            if (ready.get()) return;
            SystemClock.sleep(100);
        }
        fail("A tela não recebeu os dados esperados da API em 25 segundos.");
    }

    private static boolean containsText(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(text) && view.isShown()) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (containsText(group.getChildAt(i), text)) return true;
        }
        return false;
    }

    static String http(String method, String path, JSONObject body, AccountSession session) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BuildConfig.BACKEND_BASE_URL + path).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("Authorization", "Bearer " + session.getAccessToken());
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (java.io.OutputStream stream = connection.getOutputStream()) {
                    stream.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            int status = connection.getResponseCode();
            assertTrue("Requisição de teste recebeu HTTP " + status, status >= 200 && status < 300);
            try (java.io.InputStream stream = connection.getInputStream()) {
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = stream.read(buffer)) != -1) output.write(buffer, 0, count);
                return output.toString("UTF-8");
            }
        } finally { connection.disconnect(); }
    }

    static void awaitPurchased(String path, AccountSession session, boolean expected) throws Exception {
        long limit = SystemClock.uptimeMillis() + 15_000;
        while (SystemClock.uptimeMillis() < limit) {
            JSONArray items = new JSONArray(http("GET", path, null, session));
            if (items.length() == 1 && items.getJSONObject(0).getBoolean("purchased") == expected) return;
            SystemClock.sleep(200);
        }
        fail("A alteração de compra não foi persistida.");
    }
}
