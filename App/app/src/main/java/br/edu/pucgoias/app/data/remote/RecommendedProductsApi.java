package br.edu.pucgoias.app.data.remote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import br.edu.pucgoias.app.model.Produto;

/** Produtos reais: a chave administrativa nunca é enviada ao aplicativo. */
public final class RecommendedProductsApi {
    public interface ListCallback { void onSuccess(List<Produto> items); void onError(String message); }
    public interface PatientCallback { void onSuccess(List<Patient> patients); void onError(String message); }
    public interface ProductCallback { void onSuccess(Produto product); void onError(String message); }
    public interface DeleteCallback { void onSuccess(); void onError(String message); }
    public static final class Patient {
        public final UUID id;
        public final String name;
        Patient(UUID id, String name) { this.id = id; this.name = name; }
    }

    public void patients(String token, PatientCallback callback) {
        BackendApi.get("/api/v1/nutritionists/me/recommended-product-patients", token, new BackendApi.Callback() {
            public void onSuccess(String response) {
                try {
                    JSONArray json = new JSONArray(response);
                    List<Patient> result = new ArrayList<>();
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject p = json.getJSONObject(i);
                        result.add(new Patient(UUID.fromString(p.getString("id")), p.getString("name")));
                    }
                    callback.onSuccess(result);
                } catch (JSONException | IllegalArgumentException e) { callback.onError("Resposta inválida ao buscar pacientes."); }
            }
            public void onError(String message) { callback.onError(message); }
        });
    }

    public void list(UUID patientId, String token, ListCallback callback) {
        BackendApi.get(path(patientId), token, new BackendApi.Callback() {
            public void onSuccess(String response) {
                try {
                    JSONArray json = new JSONArray(response);
                    List<Produto> result = new ArrayList<>();
                    for (int i = 0; i < json.length(); i++) result.add(parse(json.getJSONObject(i)));
                    callback.onSuccess(result);
                } catch (JSONException | IllegalArgumentException e) { callback.onError("Resposta inválida ao buscar produtos."); }
            }
            public void onError(String message) { callback.onError(message); }
        });
    }

    public void save(UUID patientId, Produto product, String name, BigDecimal price, String link,
                     String token, ProductCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("price", price);
            body.put("purchaseUrl", link);
            BackendApi.Callback response = new BackendApi.Callback() {
                public void onSuccess(String result) {
                    try { callback.onSuccess(parse(new JSONObject(result))); }
                    catch (JSONException | IllegalArgumentException e) { callback.onError("Resposta inválida ao salvar produto."); }
                }
                public void onError(String message) { callback.onError(message); }
            };
            if (product.getId() == null) BackendApi.post(path(patientId), body, token, response);
            else BackendApi.patch(path(patientId) + "/" + product.getId(), body, token, response);
        } catch (JSONException e) { callback.onError("Não foi possível preparar o produto."); }
    }

    public void delete(UUID patientId, UUID productId, String token, DeleteCallback callback) {
        BackendApi.delete(path(patientId) + "/" + productId, token, new BackendApi.Callback() {
            public void onSuccess(String response) { callback.onSuccess(); }
            public void onError(String message) { callback.onError(message); }
        });
    }

    private static Produto parse(JSONObject json) throws JSONException {
        Produto result = new Produto(json.getString("name"), json.isNull("price") ? "" : json.get("price").toString(),
                json.isNull("purchaseUrl") ? "" : json.getString("purchaseUrl"));
        result.setId(UUID.fromString(json.getString("id")));
        return result;
    }
    private static String path(UUID id) { return "/api/v1/patients/" + id + "/recommended-products"; }
}
