package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.pucgoias.app.databinding.ItemCompraBinding;
import br.edu.pucgoias.app.model.ItemCompra;
import br.edu.pucgoias.app.util.IconeCheck;

/** Itens da lista de compras do paciente: tocar no item marca/desmarca como comprado. */
public class ItemCompraAdapter extends RecyclerView.Adapter<ItemCompraAdapter.ViewHolder> {

    private final List<ItemCompra> itens;

    public ItemCompraAdapter(List<ItemCompra> itens) {
        this.itens = itens;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCompraBinding binding = ItemCompraBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        binding.getRoot().setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao == RecyclerView.NO_POSITION) return;
            ItemCompra item = itens.get(posicao);
            item.setComprado(!item.isComprado());
            notifyItemChanged(posicao);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemCompra item = itens.get(position);
        holder.binding.tvDescricao.setText(item.getDescricao());
        IconeCheck.atualizar(holder.binding.ivAcao, item.isComprado());
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCompraBinding binding;

        ViewHolder(ItemCompraBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
