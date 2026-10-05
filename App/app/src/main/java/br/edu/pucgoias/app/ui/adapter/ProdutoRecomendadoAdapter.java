package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.pucgoias.app.databinding.ItemProdutoRecomendadoBinding;
import br.edu.pucgoias.app.model.Produto;

/** Produtos recomendados pelo nutricionista (visão do paciente). */
public class ProdutoRecomendadoAdapter extends RecyclerView.Adapter<ProdutoRecomendadoAdapter.ViewHolder> {

    public interface OnComprar {
        void onComprar(Produto produto);
    }

    private final List<Produto> produtos;
    private final OnComprar onComprar;

    public ProdutoRecomendadoAdapter(List<Produto> produtos, OnComprar onComprar) {
        this.produtos = produtos;
        this.onComprar = onComprar;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProdutoRecomendadoBinding binding = ItemProdutoRecomendadoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        binding.btnComprar.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) onComprar.onComprar(produtos.get(posicao));
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Produto produto = produtos.get(position);
        holder.binding.tvNomeProduto.setText(produto.getNome());
        holder.binding.tvPrecoProduto.setText("R$ " + produto.getPreco());
    }

    @Override
    public int getItemCount() {
        return produtos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemProdutoRecomendadoBinding binding;

        ViewHolder(ItemProdutoRecomendadoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
