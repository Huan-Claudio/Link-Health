package br.edu.pucgoias.app.ui.nutricionista.edicao;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import br.edu.pucgoias.app.data.remote.ClinicalApi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Refeições do plano do paciente, escolhidas do mesmo jeito que a tela "Planos alimentares":
 * o rascunho em edição, se houver, senão o plano publicado. (O paciente só enxerga o publicado.)
 * Usado pelo "Registro diário" para listar as refeições de cada dia.
 */
public class PlanoRefeicoesViewModel extends ViewModel {

    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    /** Refeições do plano, na mesma ordem da tela "Planos alimentares". */
    public JSONArray refeicoes = new JSONArray();
    public boolean carregando;
    @Nullable public String erro;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ClinicalApi api = new ClinicalApi();
    private boolean cleared;

    private interface Corpo { void aceitar(String body) throws JSONException; }

    public void carregar(String patientId) {
        if (carregando || patientId == null) return;
        carregando = true;
        erro = null;
        changed();
        api.request("GET", "/api/v1/me/follow-ups", null, callback(body -> {
            JSONArray links = new JSONArray(body);
            String followUp = null;
            for (int i = 0; i < links.length(); i++) {
                JSONObject f = links.getJSONObject(i);
                if (patientId.equals(f.optString("patientId")) && "ATIVO".equals(f.optString("status"))) {
                    followUp = f.getString("id");
                    break;
                }
            }
            if (followUp == null) { // sem acompanhamento ativo: não há plano
                terminar(new JSONArray());
                return;
            }
            api.request("GET", "/api/v1/follow-ups/" + followUp + "/meal-plans", null, callback(planosBody -> {
                JSONArray planos = new JSONArray(planosBody);
                JSONObject rascunho = null;
                JSONObject ativo = null;
                for (int i = 0; i < planos.length(); i++) {
                    JSONObject p = planos.getJSONObject(i);
                    if (!p.optBoolean("published")) {
                        if (rascunho == null) rascunho = p;
                    } else if (p.optBoolean("active")) {
                        ativo = p;
                    }
                }
                JSONObject escolhido = rascunho != null ? rascunho : ativo;
                JSONArray meals = escolhido == null ? null : escolhido.optJSONArray("meals");
                terminar(meals == null ? new JSONArray() : meals);
            }));
        }));
    }

    private void terminar(JSONArray meals) {
        refeicoes = meals;
        carregando = false;
        changed();
    }

    private ClinicalApi.Callback callback(Corpo corpo) {
        return new ClinicalApi.Callback() {
            @Override public void success(String body) {
                main.post(() -> {
                    if (cleared) return;
                    try {
                        corpo.aceitar(body);
                    } catch (JSONException | RuntimeException e) {
                        falhar("Resposta inválida ao consultar o plano alimentar.");
                    }
                });
            }

            @Override public void error(String message) {
                main.post(() -> {
                    if (!cleared) falhar(message);
                });
            }
        };
    }

    private void falhar(String message) {
        carregando = false;
        erro = message;
        changed();
    }

    private void changed() {
        if (!cleared) changes.setValue(changes.getValue() == null ? 1 : changes.getValue() + 1);
    }

    @Override
    protected void onCleared() {
        cleared = true;
        main.removeCallbacksAndMessages(null);
    }
}
