package br.edu.pucgoias.app.data.remote;

import br.edu.pucgoias.app.data.Sessao;
import org.json.JSONException;
import org.json.JSONObject;

/** Contratos de vínculos e prescrições. Usa somente o token da conta autenticada. */
public final class ClinicalApi {
    public interface Callback { void success(String body); void error(String message); }

    public void request(String method, String path, JSONObject body, Callback callback) {
        if (!Sessao.temSessaoRemota()) { callback.error("Entre novamente para acessar sua conta."); return; }
        BackendApi.Callback response = new BackendApi.Callback() {
            public void onSuccess(String value) { callback.success(value); }
            public void onError(String message) { callback.error(message); }
        };
        String token = Sessao.getAccessToken();
        switch (method) {
            case "GET": BackendApi.get(path, token, response); break;
            case "POST": BackendApi.post(path, body, token, response); break;
            case "PUT": BackendApi.put(path, body, token, response); break;
            case "PATCH": BackendApi.patch(path, body, token, response); break;
            case "DELETE": BackendApi.delete(path, token, response); break;
            default: throw new IllegalArgumentException("Método não suportado");
        }
    }

    public static JSONObject body(Object... values) {
        JSONObject result = new JSONObject();
        try {
            for (int i = 0; i < values.length; i += 2) result.put((String) values[i], values[i + 1]);
            return result;
        } catch (JSONException e) { throw new IllegalArgumentException(e); }
    }
}
