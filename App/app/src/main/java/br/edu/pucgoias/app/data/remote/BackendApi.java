package br.edu.pucgoias.app.data.remote;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.edu.pucgoias.app.BuildConfig;

/** Cliente HTTP único da etapa 1. A chave privada do servidor não é enviada nem armazenada no APK. */
final class BackendApi {

    private static final String LOG_TAG = "LinkHealthApi";

    interface Callback {
        void onSuccess(String body);
        void onError(String message);
    }

    private static final int TIMEOUT_MILLIS = 15_000;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);

    private BackendApi() {
    }

    static void get(String path, String token, Callback callback) {
        request("GET", path, null, token, callback);
    }

    static void post(String path, JSONObject body, @Nullable String token, Callback callback) {
        request("POST", path, body, token, callback);
    }

    static void patch(String path, JSONObject body, String token, Callback callback) {
        request("PATCH", path, body, token, callback);
    }

    static void delete(String path, String token, Callback callback) {
        request("DELETE", path, null, token, callback);
    }

    private static void request(String method, String path, @Nullable JSONObject body, @Nullable String token,
                                Callback callback) {
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String baseUrl = BuildConfig.BACKEND_BASE_URL;
                if (blank(baseUrl)) {
                    callback.onError("A conexão com o servidor ainda não está configurada.");
                    return;
                }
                connection = (HttpURLConnection) new URL(removeFinalSlash(baseUrl) + path).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(TIMEOUT_MILLIS);
                connection.setReadTimeout(TIMEOUT_MILLIS);
                connection.setRequestProperty("Accept", "application/json");
                if (!blank(token)) connection.setRequestProperty("Authorization", "Bearer " + token);
                if (body != null) {
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setDoOutput(true);
                    try (OutputStream output = connection.getOutputStream()) {
                        output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                    }
                }
                int status = connection.getResponseCode();
                String response = read(status >= 200 && status < 300
                        ? connection.getInputStream() : connection.getErrorStream());
                if (status < 200 || status >= 300) {
                    callback.onError(errorMessage(response, status));
                    return;
                }
                callback.onSuccess(response);
            } catch (IOException exception) {
                Log.e(LOG_TAG, "Falha na chamada " + method + " " + path, exception);
                callback.onError("Não foi possível conectar ao servidor. Verifique se a API está em execução.");
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private static String errorMessage(String response, int status) {
        try {
            String message = new JSONObject(response).optString("message");
            if (!blank(message)) return message;
        } catch (JSONException ignored) {
            // Resposta sem o contrato da API: usa a mensagem segura abaixo.
        }
        return status == HttpURLConnection.HTTP_UNAUTHORIZED
                ? "Sua sessão expirou. Entre novamente."
                : "Não foi possível concluir a solicitação.";
    }

    private static String read(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line);
        }
        return text.toString();
    }

    private static boolean blank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private static String removeFinalSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
