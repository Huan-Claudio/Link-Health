package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.pucgoias.app.databinding.ItemConviteBinding;

/** Convites de nutricionistas recebidos pelo paciente. */
public class ConviteAdapter extends RecyclerView.Adapter<ConviteAdapter.ViewHolder> {

    public interface OnAceitar {
        void onAceitar(String nutricionista, int posicao);
    }

    private final List<String> convites;
    private final OnAceitar onAceitar;

    public ConviteAdapter(List<String> convites, OnAceitar onAceitar) {
        this.convites = convites;
        this.onAceitar = onAceitar;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConviteBinding binding = ItemConviteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        binding.btnAdicionar.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) onAceitar.onAceitar(convites.get(posicao), posicao);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.binding.tvNome.setText(convites.get(position));
    }

    @Override
    public int getItemCount() {
        return convites.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemConviteBinding binding;

        ViewHolder(ItemConviteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
