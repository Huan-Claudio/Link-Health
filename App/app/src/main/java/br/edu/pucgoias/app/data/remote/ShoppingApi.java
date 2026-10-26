package br.edu.pucgoias.app.data.remote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/** Lista de compras persistida, acessada com a sessão do usuário. */
public final class ShoppingApi {

    public interface ListCallback {
        void onSuccess(List<Item> items);
        void onError(String message);
    }

    public interface ItemCallback {
        void onSuccess(Item item);
        void onError(String message);
    }

    public interface DeleteCallback { void onSuccess(); void onError(String message); }

    public static final class Item {
        private final UUID id;
        private final String name;
        private final String quantity;
        private final String unit;
        private final boolean purchased;

        Item(UUID id, String name, String quantity, String unit, boolean purchased) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.unit = unit;
            this.purchased = purchased;
        }

        public UUID getId() { return id; }
        public String getName() { return name; }
        public String getQuantity() { return quantity; }
        public String getUnit() { return unit; }
        public boolean isPurchased() { return purchased; }
    }

    public void list(UUID patientId, String token, ListCallback callback) {
        BackendApi.get(path(patientId), token, new BackendApi.Callback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray json = new JSONArray(response);
                    List<Item> items = new ArrayList<>();
                    for (int index = 0; index < json.length(); index++) {
                        items.add(parse(json.getJSONObject(index)));
                    }
                    callback.onSuccess(items);
                } catch (JSONException | IllegalArgumentException exception) {
                    callback.onError("A resposta do servidor para a lista de compras é inválida.");
                }
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void updatePurchaseStatus(UUID patientId, UUID itemId, boolean purchased, String token,
                                     ItemCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("purchased", purchased);
            BackendApi.patch(path(patientId) + "/" + itemId + "/purchase-status", body, token,
                    new BackendApi.Callback() {
                        @Override
                        public void onSuccess(String response) {
                            try {
                                callback.onSuccess(parse(new JSONObject(response)));
                            } catch (JSONException | IllegalArgumentException exception) {
                                callback.onError("A resposta do servidor ao atualizar o item é inválida.");
                            }
                        }

                        @Override
                        public void onError(String message) {
                            callback.onError(message);
                        }
                    });
        } catch (JSONException exception) {
            callback.onError("Não foi possível preparar a atualização do item.");
        }
    }

    public void save(UUID patientId, UUID itemId, String name, BigDecimal quantity, String unit,
                     String token, ItemCallback callback) {
        try {
            JSONObject body = new JSONObject();
            body.put("name", name);
            body.put("quantity", quantity);
            body.put("unit", unit);
            BackendApi.Callback result = new BackendApi.Callback() {
                public void onSuccess(String response) {
                    try { callback.onSuccess(parse(new JSONObject(response))); }
                    catch (JSONException | IllegalArgumentException e) { callback.onError("Resposta inválida ao salvar o item."); }
                }
                public void onError(String message) { callback.onError(message); }
            };
            if (itemId == null) BackendApi.post(path(patientId), body, token, result);
            else BackendApi.patch(path(patientId) + "/" + itemId, body, token, result);
        } catch (JSONException exception) { callback.onError("Não foi possível preparar o item."); }
    }

    public void delete(UUID patientId, UUID itemId, String token, DeleteCallback callback) {
        BackendApi.delete(path(patientId) + "/" + itemId, token, new BackendApi.Callback() {
            public void onSuccess(String response) { callback.onSuccess(); }
            public void onError(String message) { callback.onError(message); }
        });
    }

    private static Item parse(JSONObject json) throws JSONException {
        return new Item(
                UUID.fromString(json.getString("id")),
                json.getString("name"),
                json.get("quantity").toString(),
                json.getString("unit"),
                json.getBoolean("purchased"));
    }

    private static String path(UUID patientId) {
        return "/api/v1/patients/" + patientId + "/shopping-items";
    }
}
