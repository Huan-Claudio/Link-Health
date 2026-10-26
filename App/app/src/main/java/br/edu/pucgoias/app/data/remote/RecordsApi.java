package br.edu.pucgoias.app.data.remote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** Registros do paciente: todas as chamadas usam somente a sessão da conta. */
public final class RecordsApi {
    public interface Callback<T> {
        void onSuccess(T value);

        void onError(String message);
    }

    public static final String WATER = "water-intakes",
            MEALS = "meal-records",
            WEIGHT = "weight-records",
            PHOTOS = "evolution-photos",
            PLAN = "planned-meals",
            HISTORY = "records-history";
    public static final String ASSESSMENTS = "evolution-assessments";

    public void assessment(
            UUID patient,
            String token,
            JSONObject details,
            byte[][] images,
            Callback<JSONObject> callback) {
        BackendApi.uploadAssessment(
                path(patient, ASSESSMENTS), token, details, images, object(callback));
    }

    public void care(UUID patient, String token, Callback<JSONObject> callback) {
        BackendApi.get(path(patient, "care-settings"), token, object(callback));
    }

    public void saveCare(
            UUID patient, String token, JSONObject data, Callback<JSONObject> callback) {
        BackendApi.patch(path(patient, "care-settings"), data, token, object(callback));
    }

    public void waterTotal(UUID patient, String token, int total, Callback<JSONObject> callback) {
        try {
            BackendApi.post(
                    path(patient, WATER) + "/daily-total",
                    new JSONObject()
                            .put("amountMilliliters", total)
                            .put("date", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString())
                            .put("zoneId", "America/Sao_Paulo"),
                    token,
                    new BackendApi.Callback() {
                        public void onSuccess(String body) {
                            callback.onSuccess(new JSONObject());
                        }

                        public void onError(String value) {
                            callback.onError(value);
                        }
                    });
        } catch (JSONException e) {
            callback.onError("Não foi possível preparar a correção.");
        }
    }

    public void summary(UUID patient, String token, LocalDate date, Callback<JSONObject> callback) {
        BackendApi.get(
                path(patient, "records-summary") + "?date=" + date + "&zoneId=" + zone(),
                token,
                object(callback));
    }

    public void list(UUID patient, String token, String resource, Callback<JSONArray> callback) {
        BackendApi.get(
                path(patient, resource) + (HISTORY.equals(resource) ? "?zoneId=" + zone() : ""),
                token,
                new BackendApi.Callback() {
                    public void onSuccess(String body) {
                        try {
                            callback.onSuccess(new JSONArray(body));
                        } catch (JSONException e) {
                            callback.onError("Resposta inválida ao consultar os registros.");
                        }
                    }

                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
    }

    public void save(
            UUID patient,
            String token,
            String resource,
            String id,
            JSONObject body,
            Callback<JSONObject> callback) {
        if (id == null) BackendApi.post(path(patient, resource), body, token, object(callback));
        else BackendApi.patch(path(patient, resource) + "/" + id, body, token, object(callback));
    }

    public void delete(
            UUID patient, String token, String resource, String id, Callback<Void> callback) {
        BackendApi.delete(
                path(patient, resource) + "/" + id,
                token,
                new BackendApi.Callback() {
                    public void onSuccess(String body) {
                        callback.onSuccess(null);
                    }

                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
    }

    public void adjustWater(UUID patient, String token, int amount, Callback<Void> callback) {
        try {
            JSONObject body =
                    new JSONObject()
                            .put("amountMilliliters", amount)
                            .put("date", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString())
                            .put("zoneId", "America/Sao_Paulo");
            BackendApi.post(
                    path(patient, WATER) + "/adjustment",
                    body,
                    token,
                    new BackendApi.Callback() {
                        public void onSuccess(String response) {
                            callback.onSuccess(null);
                        }

                        public void onError(String message) {
                            callback.onError(message);
                        }
                    });
        } catch (JSONException e) {
            callback.onError("Não foi possível preparar o registro de água.");
        }
    }

    public void upload(
            UUID patient, String token, String id, byte[] bytes, Callback<JSONObject> callback) {
        upload(patient, token, id, bytes, "image/jpeg", callback);
    }

    public void upload(
            UUID patient,
            String token,
            String id,
            byte[] bytes,
            String mime,
            Callback<JSONObject> callback) {
        BackendApi.uploadMultipart(
                path(patient, PHOTOS) + "/" + id + "/content",
                token,
                bytes,
                "evolucao",
                mime,
                object(callback));
    }

    public void download(UUID patient, String token, String id, Callback<byte[]> callback) {
        BackendApi.download(
                path(patient, PHOTOS) + "/" + id + "/content",
                token,
                new BackendApi.BinaryCallback() {
                    public void onSuccess(byte[] bytes) {
                        callback.onSuccess(bytes);
                    }

                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
    }

    private static BackendApi.Callback object(Callback<JSONObject> callback) {
        return new BackendApi.Callback() {
            public void onSuccess(String body) {
                try {
                    callback.onSuccess(new JSONObject(body));
                } catch (JSONException e) {
                    callback.onError("Resposta inválida ao salvar os registros.");
                }
            }

            public void onError(String message) {
                callback.onError(message);
            }
        };
    }

    private static String zone() {
        return android.net.Uri.encode("America/Sao_Paulo");
    }

    private static String path(UUID patient, String resource) {
        return "/api/v1/patients/" + patient + "/" + resource;
    }
}
