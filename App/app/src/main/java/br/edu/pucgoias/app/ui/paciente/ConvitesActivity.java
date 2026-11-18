package br.edu.pucgoias.app.ui.paciente;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.databinding.ActivityConvitesBinding;
import br.edu.pucgoias.app.databinding.ItemConviteBinding;
import br.edu.pucgoias.app.ui.comum.FollowUpsViewModel;
import org.json.JSONObject;

/** Convites recebidos/enviados e estado dos acompanhamentos reais. */
public class ConvitesActivity extends BaseActivity {
    private FollowUpsViewModel model;
    private ActivityConvitesBinding binding;
    private boolean nutritionist;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        if (!Sessao.temSessaoRemota()) { finish(); return; }
        nutritionist = Sessao.getPerfil() == Perfil.NUTRICIONISTA;
        binding = ActivityConvitesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.headerSaudacao.tvOla.setText("Acompanhamentos e convites");
        binding.headerSaudacao.btnVoltar.setOnClickListener(v -> finish());
        model = new ViewModelProvider(this).get(FollowUpsViewModel.class);
        LinksAdapter adapter = new LinksAdapter();
        binding.listaConvites.setLayoutManager(new LinearLayoutManager(this));
        binding.listaConvites.setAdapter(adapter);
        binding.tvVazio.setOnClickListener(v -> model.load());
        model.changes.observe(this, ignored -> {
            adapter.notifyDataSetChanged();
            String text = model.error != null ? model.error + "\nToque para atualizar."
                    : model.pending ? "Salvando..." : model.loading ? "Carregando..."
                    : model.links.length() == 0 ? "Nenhum acompanhamento ou convite. Toque para atualizar." : "";
            binding.tvVazio.setText(text);
            binding.tvVazio.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        });
        if (!model.loaded) model.load();
    }
    @Override protected void onResume() { super.onResume(); if (model != null && model.loaded) model.load(); }
    private void select(JSONObject link) {
        String id = link.optString("id"), status = link.optString("status");
        String name = link.optString(nutritionist ? "patientName" : "nutritionistName");
        if (!nutritionist && status.equals("PENDENTE")) {
            registrarDialog(new MaterialAlertDialogBuilder(this).setTitle("Convite de " + name)
                    .setMessage("Aceitar permite que este nutricionista acompanhe seus dados e prescreva seu plano.")
                    .setPositiveButton("Aceitar", (d, w) -> model.respond(id, true))
                    .setNegativeButton("Recusar", (d, w) -> model.respond(id, false))
                    .setNeutralButton("Voltar", null).show());
        } else if (nutritionist && status.equals("ATIVO")) {
            registrarDialog(new MaterialAlertDialogBuilder(this).setTitle("Encerrar acompanhamento?")
                    .setMessage("O acesso aos registros de " + name + " será encerrado. O histórico será preservado.")
                    .setPositiveButton("Encerrar", (d, w) -> model.deactivate(id)).setNegativeButton("Cancelar", null).show());
        }
    }
    private class LinksAdapter extends RecyclerView.Adapter<Holder> {
        @Override public Holder onCreateViewHolder(ViewGroup parent, int type) { return new Holder(ItemConviteBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)); }
        @Override public void onBindViewHolder(Holder h, int position) {
            JSONObject link = model.links.optJSONObject(position);
            String name = link.optString(nutritionist ? "patientName" : "nutritionistName"), status = link.optString("status");
            h.b.tvNome.setText(name + " · " + status);
            h.b.tvAvatar.setText(name.isEmpty() ? "" : name.substring(0, 1));
            boolean actionable = nutritionist ? status.equals("ATIVO") : status.equals("PENDENTE");
            h.b.btnAdicionar.setVisibility(actionable ? View.VISIBLE : View.GONE);
            h.b.btnAdicionar.setEnabled(!model.pending && !model.loading);
            h.b.btnAdicionar.setContentDescription(nutritionist ? "Encerrar acompanhamento de " + name : "Responder convite de " + name);
            h.b.btnAdicionar.setOnClickListener(v -> select(link));
            h.itemView.setOnClickListener(v -> { if (!model.pending && !model.loading) select(link); });
        }
        @Override public int getItemCount() { return model.links.length(); }
    }
    private static class Holder extends RecyclerView.ViewHolder {
        final ItemConviteBinding b;
        Holder(ItemConviteBinding b) { super(b.getRoot()); this.b = b; }
    }
}
