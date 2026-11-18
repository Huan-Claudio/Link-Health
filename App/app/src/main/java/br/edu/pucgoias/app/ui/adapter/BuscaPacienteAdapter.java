package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import br.edu.pucgoias.app.databinding.ItemConviteBinding;
import br.edu.pucgoias.app.ui.comum.FollowUpsViewModel;
import org.json.JSONObject;

public class BuscaPacienteAdapter extends RecyclerView.Adapter<BuscaPacienteAdapter.Holder> {
    public interface Invite { void send(String id); }
    private final FollowUpsViewModel model;
    private final Invite invite;
    public BuscaPacienteAdapter(FollowUpsViewModel model, Invite invite) { this.model = model; this.invite = invite; }
    @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
        return new Holder(ItemConviteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }
    @Override public void onBindViewHolder(Holder h, int position) {
        JSONObject p = model.results.optJSONObject(position);
        String id = p.optString("id"), name = p.optString("fullName"), status = model.status(id);
        h.binding.tvNome.setText(name + (status.equals("ATIVO") ? " · Vinculado" : status.equals("PENDENTE") ? " · Convite pendente" : ""));
        h.binding.tvAvatar.setText(name.isEmpty() ? "" : name.substring(0, 1));
        h.binding.btnAdicionar.setContentDescription("Convidar " + name);
        h.binding.btnAdicionar.setEnabled(model.loaded && !model.loading && !model.pending && status.isEmpty());
        h.binding.btnAdicionar.setOnClickListener(v -> invite.send(id));
    }
    @Override public int getItemCount() { return model.results.length(); }
    static class Holder extends RecyclerView.ViewHolder {
        final ItemConviteBinding binding;
        Holder(ItemConviteBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}
