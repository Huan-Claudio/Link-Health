package br.edu.pucgoias.app.ui.comum;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Mantém uma operação em andamento ao girar a tela; nunca repete um POST automaticamente. */
public class RecordsViewModel extends ViewModel {
    private static final java.util.concurrent.ExecutorService PHOTOS = java.util.concurrent.Executors.newSingleThreadExecutor();
    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    public final Map<String, JSONArray> data = new HashMap<>();
    public JSONObject summary;
    public boolean busy;
    public int loading;
    public String error = "", message = "";
    private UUID patient;
    private String token;
    private String[] resources;
    private boolean includeSummary;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final RecordsApi api = new RecordsApi();
    private boolean cleared;

    public void configure(UUID patient, String token, boolean summary, String... resources) {
        if (this.patient != null) return;
        this.patient = patient; this.token = token; this.resources = resources; includeSummary = summary;
        reload();
    }

    public void reload() {
        if (patient == null || loading > 0 || busy) return;
        error = "";
        loading = resources.length + (includeSummary ? 1 : 0);
        notifyChanged();
        if (includeSummary) api.summary(patient, token, RecordFormatting.todayDate(), new RecordsApi.Callback<JSONObject>() {
            public void onSuccess(JSONObject value) { dispatch(() -> { summary = value; loaded(); }); }
            public void onError(String value) { dispatch(() -> { error = value; loaded(); }); }
        });
        for (String resource : resources) api.list(patient, token, resource, new RecordsApi.Callback<JSONArray>() {
            public void onSuccess(JSONArray value) { dispatch(() -> { data.put(resource, value); loaded(); }); }
            public void onError(String value) { dispatch(() -> { error = value; loaded(); }); }
        });
    }

    public JSONArray items(String resource) { return data.getOrDefault(resource, new JSONArray()); }
    public boolean ready() { return !busy && loading == 0; }
    public void save(String resource, String id, JSONObject body) {
        if (!begin()) return;
        api.save(patient, token, resource, id, body, mutation("Registro salvo."));
    }
    public void delete(String resource, String id) {
        if (!begin()) return;
        api.delete(patient, token, resource, id, mutation("Registro removido."));
    }
    public void water(int amount) {
        if (!begin()) return;
        api.adjustWater(patient, token, amount, mutation("Consumo de água atualizado."));
    }
    public void photo(byte[] bytes, String existingId) {
        if (!begin()) return;
        sendPhoto(bytes, existingId, photoMime(existingId));
    }
    public void preparePhoto(android.content.ContentResolver resolver, android.net.Uri uri, String existingId, java.io.File cameraFile) {
        if (!begin()) return;
        String mime = photoMime(existingId);
        PHOTOS.execute(() -> {
            try {
                byte[] bytes = PhotoPreparation.read(resolver, uri, mime);
                dispatch(() -> sendPhoto(bytes, existingId, mime));
            } catch (java.io.IOException | RuntimeException e) {
                dispatch(() -> failed(e.getMessage() == null ? "Não foi possível preparar a foto." : e.getMessage()));
            } finally { if (cameraFile != null) cameraFile.delete(); }
        });
    }
    private String photoMime(String id) {
        JSONArray photos = items(RecordsApi.PHOTOS);
        for (int i = 0; i < photos.length(); i++) {
            JSONObject photo = photos.optJSONObject(i);
            if (photo != null && photo.optString("id").equals(id)) return photo.optString("contentType", "image/jpeg");
        }
        return "image/jpeg";
    }
    private void sendPhoto(byte[] bytes, String existingId, String mime) {
        if (existingId != null) { upload(existingId, bytes, mime); return; }
        try {
            JSONObject body = new JSONObject().put("originalFileName", "evolucao.jpg").put("contentType", "image/jpeg");
            api.save(patient, token, RecordsApi.PHOTOS, null, body, new RecordsApi.Callback<JSONObject>() {
                public void onSuccess(JSONObject value) { upload(value.optString("id"), bytes, "image/jpeg"); }
                public void onError(String value) { dispatch(() -> failed(value)); }
            });
        } catch (JSONException e) { failed("Não foi possível preparar a foto."); }
    }
    private void upload(String id, byte[] bytes, String mime) {
        api.upload(patient, token, id, bytes, mime, new RecordsApi.Callback<JSONObject>() {
            public void onSuccess(JSONObject value) { dispatch(() -> success("Foto enviada com segurança.")); }
            public void onError(String value) { dispatch(() -> {
                busy = false;
                String failure = value + " O registro ficou pendente; toque nele para tentar enviar novamente.";
                reload(); error = failure; notifyChanged();
            }); }
        });
    }
    public void report(String value) { error = value; notifyChanged(); }
    private <T> RecordsApi.Callback<T> mutation(String success) {
        return new RecordsApi.Callback<T>() {
            public void onSuccess(T value) { dispatch(() -> success(success)); }
            public void onError(String value) { dispatch(() -> failed(value)); }
        };
    }
    private boolean begin() { if (!ready()) return false; busy = true; error = ""; message = ""; notifyChanged(); return true; }
    private void success(String value) { busy = false; message = value; reload(); }
    private void failed(String value) { busy = false; error = value; notifyChanged(); }
    private void loaded() { loading--; notifyChanged(); }
    private void dispatch(Runnable action) { main.post(() -> { if (!cleared) action.run(); }); }
    private void notifyChanged() { changes.setValue(changes.getValue() == null ? 1 : changes.getValue() + 1); }
    @Override protected void onCleared() { cleared = true; }
}
