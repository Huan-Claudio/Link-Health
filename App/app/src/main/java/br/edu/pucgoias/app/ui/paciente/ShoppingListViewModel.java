package br.edu.pucgoias.app.ui.paciente;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import br.edu.pucgoias.app.data.remote.RecommendedProductsApi;
import br.edu.pucgoias.app.data.remote.ShoppingApi;
import br.edu.pucgoias.app.model.ItemCompra;
import br.edu.pucgoias.app.model.Produto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Confirmação no servidor antes da marcação e operação preservada ao girar a tela. */
public final class ShoppingListViewModel extends ViewModel {
    public final List<ItemCompra> items = new ArrayList<>();
    public final List<Produto> products = new ArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    private final ShoppingApi api = new ShoppingApi();
    private UUID patient;
    private String token;
    public boolean loading, pending, productsLoaded;
    public String status = "Carregando lista…";
    private String message;
    private int generation;

    public LiveData<Integer> changes() {
        return changes;
    }

    public String takeMessage() {
        String result = message;
        message = null;
        return result;
    }

    private void publish() {
        changes.setValue(changes.getValue() + 1);
    }

    public void load(UUID id, String accessToken) {
        if (loading || pending) return;
        patient = id;
        token = accessToken;
        loading = true;
        productsLoaded = false;
        status = "Carregando lista…";
        publish();
        int request = ++generation;
        api.list(
                id,
                token,
                new ShoppingApi.ListCallback() {
                    public void onSuccess(List<ShoppingApi.Item> result) {
                        main.post(
                                () -> {
                                    if (request != generation) return;
                                    items.clear();
                                    for (ShoppingApi.Item item : result)
                                        items.add(
                                                new ItemCompra(
                                                        item.getId(),
                                                        description(item),
                                                        item.isPurchased()));
                                    loading = false;
                                    status =
                                            items.isEmpty()
                                                    ? "Sua lista está vazia. Toque em Editar minha"
                                                          + " lista para adicionar itens."
                                                    : "Toque nos itens para marcar ou desmarcar a"
                                                          + " compra.";
                                    publish();
                                });
                    }

                    public void onError(String error) {
                        main.post(
                                () -> {
                                    if (request != generation) return;
                                    loading = false;
                                    items.clear();
                                    status = error + " Toque aqui para tentar novamente.";
                                    publish();
                                });
                    }
                });
        new RecommendedProductsApi()
                .list(
                        id,
                        token,
                        new RecommendedProductsApi.ListCallback() {
                            public void onSuccess(List<Produto> result) {
                                main.post(
                                        () -> {
                                            if (request != generation) return;
                                            products.clear();
                                            products.addAll(result);
                                            productsLoaded = true;
                                            publish();
                                        });
                            }

                            public void onError(String error) {
                                main.post(
                                        () -> {
                                            if (request != generation) return;
                                            products.clear();
                                            message = error;
                                            publish();
                                        });
                            }
                        });
    }

    public void toggle(ItemCompra item) {
        if (pending || loading || item.getId() == null) return;
        pending = true;
        status = "Atualizando compra…";
        publish();
        api.updatePurchaseStatus(
                patient,
                item.getId(),
                !item.isComprado(),
                token,
                new ShoppingApi.ItemCallback() {
                    public void onSuccess(ShoppingApi.Item result) {
                        main.post(
                                () -> {
                                    item.setComprado(result.isPurchased());
                                    pending = false;
                                    status =
                                            result.isPurchased()
                                                    ? "Item marcado como comprado."
                                                    : "Item desmarcado.";
                                    publish();
                                });
                    }

                    public void onError(String error) {
                        main.post(
                                () -> {
                                    pending = false;
                                    status = error;
                                    message = error;
                                    publish();
                                });
                    }
                });
    }

    private static String description(ShoppingApi.Item item) {
        String unit;
        switch (item.getUnit()) {
            case "GRAM":
                unit = "g";
                break;
            case "KILOGRAM":
                unit = "kg";
                break;
            case "MILLILITER":
                unit = "ml";
                break;
            case "LITER":
                unit = "L";
                break;
            case "PACKAGE":
                unit = "pacote(s)";
                break;
            case "BOX":
                unit = "caixa(s)";
                break;
            default:
                unit = "unidade(s)";
        }
        return item.getQuantity() + " " + unit + " de " + item.getName();
    }
}
