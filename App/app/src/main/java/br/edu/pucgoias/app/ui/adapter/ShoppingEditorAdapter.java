package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.Arrays;
import java.util.List;
import br.edu.pucgoias.app.databinding.ItemShoppingEditorBinding;
import br.edu.pucgoias.app.ui.nutricionista.edicao.ShoppingDraft;
import br.edu.pucgoias.app.util.TextoAlterado;

public class ShoppingEditorAdapter extends RecyclerView.Adapter<ShoppingEditorAdapter.Holder> {
    public interface Actions { void save(ShoppingDraft item); void remove(ShoppingDraft item); }
    private final List<ShoppingDraft> items;
    private final Actions actions;
    public ShoppingEditorAdapter(List<ShoppingDraft> items, Actions actions) { this.items = items; this.actions = actions; }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        ItemShoppingEditorBinding b = ItemShoppingEditorBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        Holder h = new Holder(b);
        ArrayAdapter<String> units = new ArrayAdapter<>(parent.getContext(), android.R.layout.simple_spinner_item, ShoppingDraft.LABELS);
        units.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        b.spShoppingUnit.setAdapter(units);
        b.etShoppingName.addTextChangedListener(new TextoAlterado(t -> { if (h.item != null) h.item.name = t; }));
        b.etShoppingQuantity.addTextChangedListener(new TextoAlterado(t -> { if (h.item != null) h.item.quantity = t; }));
        b.spShoppingUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
                if (h.item != null) h.item.unit = ShoppingDraft.UNITS[position];
            }
            public void onNothingSelected(AdapterView<?> p) { }
        });
        b.btnShoppingSave.setOnClickListener(v -> { if (h.item != null) actions.save(h.item); });
        b.btnShoppingDelete.setOnClickListener(v -> { if (h.item != null) actions.remove(h.item); });
        return h;
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        ShoppingDraft item = items.get(position); h.item = null;
        h.b.etShoppingName.setText(item.name); h.b.etShoppingQuantity.setText(item.quantity);
        h.b.spShoppingUnit.setSelection(Math.max(0, Arrays.asList(ShoppingDraft.UNITS).indexOf(item.unit)), false);
        h.b.tvShoppingStatus.setText(item.id == null ? "Novo item" : item.purchased ? "Comprado" : "A comprar");
        h.b.btnShoppingSave.setText(item.pending ? "Aguarde…" : "Salvar item");
        h.b.etShoppingName.setEnabled(!item.pending); h.b.etShoppingQuantity.setEnabled(!item.pending);
        h.b.spShoppingUnit.setEnabled(!item.pending); h.b.btnShoppingSave.setEnabled(!item.pending);
        h.b.btnShoppingDelete.setEnabled(!item.pending); h.item = item;
    }
    @Override public int getItemCount() { return items.size(); }
    static class Holder extends RecyclerView.ViewHolder {
        final ItemShoppingEditorBinding b; ShoppingDraft item;
        Holder(ItemShoppingEditorBinding b) { super(b.getRoot()); this.b = b; }
    }
}
