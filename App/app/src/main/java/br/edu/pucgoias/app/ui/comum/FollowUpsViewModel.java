package br.edu.pucgoias.app.ui.comum;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.ClinicalApi;
import org.json.*;
import java.net.URLEncoder;

/** Mantém as respostas e operações durante a rotação; busca antiga nunca sobrescreve a atual. */
public class FollowUpsViewModel extends ViewModel {
    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    public JSONArray links = new JSONArray(), results = new JSONArray();
    public boolean loading, searching, pending, loaded;
    public String error, query = "";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ClinicalApi api = new ClinicalApi();
    private int generation;
    private boolean cleared;

    private void changed() { if (!cleared) changes.setValue(changes.getValue() + 1); }
    public void load() {
        if (loading || pending) return;
        loading = true; error = null; changed();
        api.request("GET", "/api/v1/me/follow-ups", null, callback(body -> {
            links = new JSONArray(body); loaded = true;
        }, false));
    }
    public void search(String text) {
        query = text.trim();
        int version = ++generation;
        results = new JSONArray(); searching = !query.isEmpty(); error = null; changed();
        if (query.isEmpty()) return;
        final String term = query;
        main.postDelayed(() -> {
            if (cleared || version != generation) return;
            try {
                api.request("GET", "/api/v1/patients/search?query=" + URLEncoder.encode(term, "UTF-8"), null,
                        new ClinicalApi.Callback() {
                    public void success(String body) { main.post(() -> {
                        if (cleared || version != generation) return;
                        try { results = new JSONArray(body); } catch (JSONException e) { error = "Resposta inválida do servidor."; }
                        searching = false; changed();
                    }); }
                    public void error(String message) { main.post(() -> {
                        if (cleared || version != generation) return;
                        searching = false; error = message; changed();
                    }); }
                });
            } catch (java.io.UnsupportedEncodingException e) { searching = false; error = "Não foi possível buscar."; changed(); }
        }, 350);
    }
    public String status(String patientId) {
        for (int i = 0; i < links.length(); i++) {
            JSONObject link = links.optJSONObject(i);
            String status = link.optString("status");
            if (patientId.equals(link.optString("patientId")) && (status.equals("ATIVO") || status.equals("PENDENTE"))) return status;
        }
        return "";
    }
    public void invite(String patient) {
        mutate("/api/v1/follow-ups", ClinicalApi.body("nutritionistId", Sessao.getUsuarioId(), "patientId", patient));
    }
    public void respond(String id, boolean accept) {
        mutate("/api/v1/follow-ups/" + id + "/response", ClinicalApi.body("patientId", Sessao.getUsuarioId(), "accept", accept));
    }
    public void deactivate(String id) {
        mutate("/api/v1/follow-ups/" + id + "/deactivate", ClinicalApi.body("nutritionistId", Sessao.getUsuarioId()));
    }
    private void mutate(String path, JSONObject body) {
        if (pending || loading) return;
        pending = true; error = null; changed();
        api.request("POST", path, body, callback(value -> {}, true));
    }
    private interface Result { void accept(String body) throws JSONException; }
    private ClinicalApi.Callback callback(Result result, boolean reload) {
        return new ClinicalApi.Callback() {
            public void success(String body) { main.post(() -> {
                if (cleared) return;
                loading = false; pending = false;
                try { result.accept(body); } catch (JSONException e) { error = "Resposta inválida do servidor."; }
                changed(); if (reload) load();
            }); }
            public void error(String message) { main.post(() -> {
                if (cleared) return;
                loading = false; pending = false; error = message; changed();
            }); }
        };
    }
    @Override protected void onCleared() { cleared = true; generation++; main.removeCallbacksAndMessages(null); }
}
