package br.edu.pucgoias.app.ui.nutricionista.edicao;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.edu.pucgoias.app.data.remote.ClinicalApi;
import org.json.*;

public class PlansViewModel extends ViewModel {
    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    public JSONArray plans = new JSONArray();
    public String patient, followUp, selectedId, error;
    public boolean busy, loaded;
    public String draftKey = "", draftName = "", draftTime = "";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ClinicalApi api = new ClinicalApi();
    private boolean cleared;

    public void start(String id) { if (patient != null) return; patient = id; load(); }
    public JSONObject selected() {
        for (int i = 0; i < plans.length(); i++) {
            JSONObject p = plans.optJSONObject(i);
            if (p.optString("id").equals(selectedId)) return p;
        }
        return null;
    }
    public void select(String id) { if (busy) return; selectedId = id; error = null; changed(); }
    public void load() {
        if (busy) return;
        busy = true; error = null; changed();
        api.request("GET", "/api/v1/me/follow-ups", null, callback(body -> {
            JSONArray links = new JSONArray(body); followUp = null;
            for (int i = 0; i < links.length(); i++) {
                JSONObject f = links.getJSONObject(i);
                if (patient.equals(f.optString("patientId")) && f.optString("status").equals("ATIVO")) { followUp = f.getString("id"); break; }
            }
            if (followUp == null) { plans = new JSONArray(); finish("É necessário um acompanhamento ativo para gerenciar planos."); return; }
            loadPlans();
        }));
    }
    private void loadPlans() {
        api.request("GET", "/api/v1/follow-ups/" + followUp + "/meal-plans", null, callback(body -> {
            plans = new JSONArray(body); loaded = true;
            if (selected() == null) selectedId = plans.length() == 0 ? null : plans.getJSONObject(0).getString("id");
            finish(null);
        }));
    }
    public void create() { mutate("POST", "/api/v1/follow-ups/" + followUp + "/meal-plans", null); }
    public void mutate(String method, String path, JSONObject data) {
        if (busy || followUp == null) return;
        busy = true; error = null; changed();
        api.request(method, path, data, callback(body -> {
            if (!body.isEmpty()) selectedId = new JSONObject(body).getString("id");
            draftKey = "";
            loadPlans();
        }));
    }
    private interface Result { void accept(String body) throws JSONException; }
    private ClinicalApi.Callback callback(Result result) {
        return new ClinicalApi.Callback() {
            public void success(String body) { main.post(() -> {
                if (cleared) return;
                try { result.accept(body); } catch (JSONException e) { finish("Resposta inválida. Atualize antes de repetir a operação."); }
            }); }
            public void error(String message) { main.post(() -> { if (!cleared) finish(message + " Atualize para conferir o estado salvo antes de repetir."); }); }
        };
    }
    private void finish(String message) { busy = false; error = message; changed(); }
    private void changed() { if (!cleared) changes.setValue(changes.getValue() + 1); }
    @Override protected void onCleared() { cleared = true; main.removeCallbacksAndMessages(null); }
}
