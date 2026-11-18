package br.edu.pucgoias.app.ui.comum;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import br.edu.pucgoias.app.data.remote.ClinicalApi;
import br.edu.pucgoias.app.data.Sessao;
import org.json.*;

public class ProfileViewModel extends ViewModel {
    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    public String name = "", email = "", phone = "", birth = "", currentPassword = "", newPassword = "", error;
    public boolean loaded, busy, done, reauthenticate;
    private String originalEmail;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ClinicalApi api = new ClinicalApi();
    private boolean cleared;
    public void load() {
        if (busy) return;
        busy = true; error = null; changed();
        api.request("GET", "/api/v1/me", null, callback(false));
    }
    public void save(String date) {
        if (busy || !loaded) return;
        busy = true; error = null;
        reauthenticate = !email.trim().equalsIgnoreCase(originalEmail) || !newPassword.isEmpty();
        changed();
        api.request("PATCH", "/api/v1/me", ClinicalApi.body("fullName", name.trim(), "email", email.trim(),
                "phone", phone.trim(), "birthDate", date.isEmpty() ? JSONObject.NULL : date,
                "currentPassword", currentPassword.isEmpty() ? JSONObject.NULL : currentPassword,
                "newPassword", newPassword.isEmpty() ? JSONObject.NULL : newPassword), callback(true));
    }
    private ClinicalApi.Callback callback(boolean save) {
        return new ClinicalApi.Callback() {
            public void success(String body) { main.post(() -> {
                if (cleared) return;
                busy = false;
                try {
                    JSONObject value = new JSONObject(body);
                    name = value.optString("fullName"); email = value.optString("email"); originalEmail = email;
                    phone = value.optString("phone", "");
                    String date = value.optString("birthDate", "");
                    birth = date.isEmpty() ? "" : java.time.LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"));
                    loaded = true;
                    if (save) { Sessao.setNomeUsuario(name); currentPassword = ""; newPassword = ""; done = true; }
                } catch (Exception e) { error = "Não foi possível ler o perfil retornado. Entre novamente se alterou e-mail/senha."; }
                changed();
            }); }
            public void error(String message) { main.post(() -> {
                if (cleared) return;
                busy = false; error = message; changed();
            }); }
        };
    }
    private void changed() { if (!cleared) changes.setValue(changes.getValue() + 1); }
    @Override protected void onCleared() { cleared = true; currentPassword = ""; newPassword = ""; main.removeCallbacksAndMessages(null); }
}
