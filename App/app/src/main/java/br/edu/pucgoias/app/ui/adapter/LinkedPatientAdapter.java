package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import br.edu.pucgoias.app.data.remote.RecommendedProductsApi;
import br.edu.pucgoias.app.databinding.ItemPacienteBinding;

/** Somente contas devolvidas pelo backend para o nutricionista autenticado. */
public final class LinkedPatientAdapter extends RecyclerView.Adapter<LinkedPatientAdapter.Holder> {
    public interface Action { void open(RecommendedProductsApi.Patient patient); }
    private final List<RecommendedProductsApi.Patient> patients = new ArrayList<>();
    private final Action action;
    public LinkedPatientAdapter(Action action) { this.action = action; }
    public void update(List<RecommendedProductsApi.Patient> values) {
        patients.clear(); patients.addAll(values); notifyDataSetChanged();
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        ItemPacienteBinding b = ItemPacienteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        Holder h = new Holder(b);
        b.getRoot().setOnClickListener(v -> { int i = h.getBindingAdapterPosition(); if (i != RecyclerView.NO_POSITION) action.open(patients.get(i)); });
        return h;
    }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        RecommendedProductsApi.Patient p = patients.get(position);
        String[] names = p.name.trim().split("\\s+");
        String initials = names[0].isEmpty() ? "P" : names[0].substring(0, 1);
        if (names.length > 1) initials += names[names.length - 1].substring(0, 1);
        h.b.tvAvatar.setText(initials.toUpperCase(Locale.ROOT));
        h.b.tvNome.setText(p.name); h.b.tvObjetivo.setText("Acompanhamento ativo");
    }
    @Override public int getItemCount() { return patients.size(); }
    static class Holder extends RecyclerView.ViewHolder {
        final ItemPacienteBinding b;
        Holder(ItemPacienteBinding b) { super(b.getRoot()); this.b = b; }
    }
}
