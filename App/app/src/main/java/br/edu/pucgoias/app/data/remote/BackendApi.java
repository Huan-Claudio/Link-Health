package br.edu.pucgoias.app.data.remote;

import android.util.Log;

import androidx.annotation.Nullable;

import br.edu.pucgoias.app.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Transporte autenticado. A chave administrativa nunca é enviada nem armazenada no APK. */
final class BackendApi {

    private static final String LOG_TAG = "LinkHealthApi";

    interface Callback {
        void onSuccess(String body);

        void onError(String message);
    }

    interface BinaryCallback {
        void onSuccess(byte[] bytes);

        void onError(String message);
    }

    private static final int TIMEOUT_MILLIS = 15_000;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final ExecutorService FILE_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;

    private BackendApi() {}

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

    static void download(String path, String token, BinaryCallback callback) {
        FILE_EXECUTOR.execute(
                () -> {
                    HttpURLConnection connection = null;
                    try {
                        connection = open("GET", path, token);
                        connection.setRequestProperty("Accept", "image/jpeg, image/png");
                        int status = connection.getResponseCode();
                        if (status < 200 || status >= 300) {
                            callback.onError(
                                    errorMessage(read(connection.getErrorStream()), status));
                            return;
                        }
                        if (connection.getContentLengthLong() > MAX_PHOTO_BYTES) {
                            callback.onError("A foto ultrapassa o limite de 5 MB.");
                            return;
                        }
                        String type = connection.getContentType();
                        if (type == null
                                || !(type.startsWith("image/jpeg")
                                        || type.startsWith("image/png"))) {
                            callback.onError("O servidor não retornou uma foto JPEG ou PNG.");
                            return;
                        }
                        try (InputStream input = connection.getInputStream();
                                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                            byte[] buffer = new byte[8192];
                            int count;
                            while ((count = input.read(buffer)) != -1) {
                                if (output.size() + count > MAX_PHOTO_BYTES)
                                    throw new IOException("Photo too large");
                                output.write(buffer, 0, count);
                            }
                            callback.onSuccess(output.toByteArray());
                        }
                    } catch (IOException exception) {
                        callback.onError(
                                "Não foi possível carregar a foto. Confira a conexão e tente"
                                    + " novamente.");
                    } finally {
                        if (connection != null) connection.disconnect();
                    }
                });
    }

    static void uploadMultipart(
            String path,
            String token,
            byte[] bytes,
            String filename,
            String mime,
            Callback callback) {
        if (bytes == null
                || bytes.length == 0
                || bytes.length > MAX_PHOTO_BYTES
                || !("image/jpeg".equals(mime) || "image/png".equals(mime))) {
            callback.onError("Selecione uma foto JPEG ou PNG de até 5 MB.");
            return;
        }
        FILE_EXECUTOR.execute(
                () -> {
                    HttpURLConnection connection = null;
                    try {
                        connection = open("POST", path, token);
                        String boundary =
                                "LinkHealth" + UUID.randomUUID().toString().replace("-", "");
                        // Nome fixo impede injeção de cabeçalhos e evita enviar caminhos pessoais.
                        String safeName =
                                "image/png".equals(mime) ? "evolucao.png" : "evolucao.jpg";
                        byte[] prefix =
                                ("--"
                                                + boundary
                                                + "\r\n"
                                                + "Content-Disposition: form-data; name=\"file\";"
                                                + " filename=\""
                                                + safeName
                                                + "\"\r\nContent-Type: "
                                                + mime
                                                + "\r\n\r\n")
                                        .getBytes(StandardCharsets.UTF_8);
                        byte[] suffix =
                                ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
                        connection.setRequestProperty(
                                "Content-Type", "multipart/form-data; boundary=" + boundary);
                        connection.setFixedLengthStreamingMode(
                                prefix.length + bytes.length + suffix.length);
                        connection.setDoOutput(true);
                        try (OutputStream output = connection.getOutputStream()) {
                            output.write(prefix);
                            output.write(bytes);
                            output.write(suffix);
                        }
                        int status = connection.getResponseCode();
                        String response =
                                read(
                                        status >= 200 && status < 300
                                                ? connection.getInputStream()
                                                : connection.getErrorStream());
                        if (status >= 200 && status < 300) callback.onSuccess(response);
                        else callback.onError(errorMessage(response, status));
                    } catch (IOException exception) {
                        callback.onError(
                                "Não foi possível confirmar o envio da foto. Atualize a tela antes"
                                    + " de tentar novamente.");
                    } finally {
                        if (connection != null) connection.disconnect();
                    }
                });
    }

    private static HttpURLConnection open(String method, String path, @Nullable String token)
            throws IOException {
        String baseUrl = BuildConfig.BACKEND_BASE_URL;
        if (blank(baseUrl)) throw new IOException("Backend not configured");
        HttpURLConnection connection =
                (HttpURLConnection) new URL(removeFinalSlash(baseUrl) + path).openConnection();
        // Não encaminha tokens para outro servidor em redirecionamentos.
        connection.setInstanceFollowRedirects(false);
        connection.setUseCaches(false);
        connection.setRequestMethod(method);
        connection.setConnectTimeout(TIMEOUT_MILLIS);
        connection.setReadTimeout(TIMEOUT_MILLIS);
        connection.setRequestProperty("Accept", "application/json");
        if (!blank(token)) connection.setRequestProperty("Authorization", "Bearer " + token);
        return connection;
    }

    static void uploadAssessment(
            String path, String token, JSONObject details, byte[][] images, Callback callback) {
        FILE_EXECUTOR.execute(
                () -> {
                    HttpURLConnection connection = null;
                    try {
                        connection = open("POST", path, token);
                        String boundary =
                                "Assessment" + UUID.randomUUID().toString().replace("-", "");
                        ByteArrayOutputStream body = new ByteArrayOutputStream();
                        body.write(
                                ("--"
                                                + boundary
                                                + "\r\n"
                                                + "Content-Disposition: form-data;"
                                                + " name=\"details\"\r\n"
                                                + "Content-Type: application/json\r\n\r\n"
                                                + details
                                                + "\r\n")
                                        .getBytes(StandardCharsets.UTF_8));
                        String[] names = {"front", "side", "back"};
                        for (int i = 0; i < 3; i++) {
                            if (images[i] == null
                                    || images[i].length == 0
                                    || images[i].length > MAX_PHOTO_BYTES)
                                throw new IOException("Invalid image");
                            body.write(
                                    ("--"
                                                    + boundary
                                                    + "\r\nContent-Disposition: form-data; name=\""
                                                    + names[i]
                                                    + "\"; filename=\""
                                                    + names[i]
                                                    + ".jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
                                            .getBytes(StandardCharsets.UTF_8));
                            body.write(images[i]);
                            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
                        }
                        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
                        connection.setRequestProperty(
                                "Content-Type", "multipart/form-data; boundary=" + boundary);
                        connection.setFixedLengthStreamingMode(body.size());
                        connection.setDoOutput(true);
                        try (OutputStream out = connection.getOutputStream()) {
                            body.writeTo(out);
                        }
                        int status = connection.getResponseCode();
                        String response =
                                read(
                                        status >= 200 && status < 300
                                                ? connection.getInputStream()
                                                : connection.getErrorStream());
                        if (status >= 200 && status < 300) callback.onSuccess(response);
                        else callback.onError(errorMessage(response, status));
                    } catch (IOException e) {
                        callback.onError(
                                "Não foi possível confirmar o registro. Tente salvar novamente; o"
                                    + " mesmo envio não será duplicado.");
                    } finally {
                        if (connection != null) connection.disconnect();
                    }
                });
    }

    private static void request(
            String method,
            String path,
            @Nullable JSONObject body,
            @Nullable String token,
            Callback callback) {
        EXECUTOR.execute(
                () -> {
                    HttpURLConnection connection = null;
                    try {
                        String baseUrl = BuildConfig.BACKEND_BASE_URL;
                        if (blank(baseUrl)) {
                            callback.onError(
                                    "A conexão com o servidor ainda não está configurada.");
                            return;
                        }
                        connection = open(method, path, token);
                        if (body != null) {
                            connection.setRequestProperty(
                                    "Content-Type", "application/json; charset=utf-8");
                            connection.setDoOutput(true);
                            try (OutputStream output = connection.getOutputStream()) {
                                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
                            }
                        }
                        int status = connection.getResponseCode();
                        String response =
                                read(
                                        status >= 200 && status < 300
                                                ? connection.getInputStream()
                                                : connection.getErrorStream());
                        if (status < 200 || status >= 300) {
                            callback.onError(errorMessage(response, status));
                            return;
                        }
                        callback.onSuccess(response);
                    } catch (IOException exception) {
                        Log.e(LOG_TAG, "Falha na chamada " + method + " " + path, exception);
                        callback.onError(
                                "Não foi possível conectar ao servidor. Verifique se a API está em"
                                    + " execução.");
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
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
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
