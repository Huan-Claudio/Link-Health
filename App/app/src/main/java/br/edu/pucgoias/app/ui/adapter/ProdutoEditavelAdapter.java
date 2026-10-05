package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ItemProdutoEditavelBinding;
import br.edu.pucgoias.app.ui.nutricionista.edicao.RascunhoProduto;
import br.edu.pucgoias.app.util.TextoAlterado;

/** Formulários "Recomendar Produto" da tela Editar Produtos Recomendados. */
public class ProdutoEditavelAdapter extends RecyclerView.Adapter<ProdutoEditavelAdapter.ViewHolder> {

    public interface Acoes {
        void salvar(int posicao);
        void remover(int posicao);
    }

    private final List<RascunhoProduto> produtos;
    private final Acoes acoes;

    public ProdutoEditavelAdapter(List<RascunhoProduto> produtos, Acoes acoes) {
        this.produtos = produtos;
        this.acoes = acoes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProdutoEditavelBinding binding = ItemProdutoEditavelBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        // Os campos escrevem no rascunho ligado ao ViewHolder neste momento.
        binding.etNomeProduto.addTextChangedListener(new TextoAlterado(t -> {
            if (holder.rascunho != null) holder.rascunho.nome = t;
        }));
        binding.etPrecoProduto.addTextChangedListener(new TextoAlterado(t -> {
            if (holder.rascunho != null) holder.rascunho.preco = t;
        }));
        binding.etLinkProduto.addTextChangedListener(new TextoAlterado(t -> {
            if (holder.rascunho != null) holder.rascunho.link = t;
        }));
        binding.btnSalvarProduto.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao == RecyclerView.NO_POSITION) return;
            if (produtos.get(posicao).nome.trim().isEmpty()) {
                binding.etNomeProduto.setError(v.getContext().getString(R.string.campo_obrigatorio));
                return;
            }
            acoes.salvar(posicao);
        });
        binding.btnRemover.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) acoes.remover(posicao);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RascunhoProduto r = produtos.get(position);
        holder.rascunho = null; // evita gravar no rascunho anterior durante o setText
        holder.binding.etNomeProduto.setText(r.nome);
        holder.binding.etPrecoProduto.setText(r.preco);
        holder.binding.etLinkProduto.setText(r.link);
        holder.binding.etNomeProduto.setError(null);
        holder.rascunho = r;
    }

    @Override
    public int getItemCount() {
        return produtos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemProdutoEditavelBinding binding;
        RascunhoProduto rascunho;

        ViewHolder(ItemProdutoEditavelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
