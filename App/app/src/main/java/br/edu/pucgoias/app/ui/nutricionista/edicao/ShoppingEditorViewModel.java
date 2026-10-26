package br.edu.pucgoias.app.ui.nutricionista.edicao;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import br.edu.pucgoias.app.data.remote.ShoppingApi;

/** O pedido e seu resultado sobrevivem à rotação sem repetir a criação do item. */
public class ShoppingEditorViewModel extends ViewModel {
    public final List<ShoppingDraft> items = new ArrayList<>();
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ShoppingApi api = new ShoppingApi();
    private UUID patient;
    private String token;
    public boolean loading, loaded;
    private String message;

    public LiveData<Integer> changes() { return changes; }
    public String takeMessage() { String result = message; message = null; return result; }
    private void publish(String text) { message = text; changes.setValue(changes.getValue() + 1); }
    public void load(UUID id, String accessToken) {
        if (loading || loaded) return;
        patient = id; token = accessToken; loading = true; publish(null);
        api.list(id, token, new ShoppingApi.ListCallback() {
            public void onSuccess(List<ShoppingApi.Item> result) { main.post(() -> {
                items.clear(); for (ShoppingApi.Item item : result) items.add(new ShoppingDraft(item));
                loaded = true; loading = false; publish(null);
            }); }
            public void onError(String error) { main.post(() -> { loading = false; publish(error); }); }
        });
    }
    public void add() { if (loaded) { items.add(new ShoppingDraft()); publish(null); } }
    public void save(ShoppingDraft item) {
        if (item.pending) return;
        String error = item.validationError();
        if (error != null) { publish(error); return; }
        item.pending = true; publish(null);
        api.save(patient, item.id, item.name.trim(), item.parsedQuantity(), item.unit, token, new ShoppingApi.ItemCallback() {
            public void onSuccess(ShoppingApi.Item result) { main.post(() -> {
                item.accept(result); item.pending = false; publish("Item salvo.");
            }); }
            public void onError(String error) { main.post(() -> { item.pending = false; publish(error); }); }
        });
    }
    public void remove(ShoppingDraft item) {
        if (item.pending) return;
        if (item.id == null) { items.remove(item); publish(null); return; }
        item.pending = true; publish(null);
        api.delete(patient, item.id, token, new ShoppingApi.DeleteCallback() {
            public void onSuccess() { main.post(() -> { items.remove(item); publish("Item removido."); }); }
            public void onError(String error) { main.post(() -> { item.pending = false; publish(error); }); }
        });
    }
}
