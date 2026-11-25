package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ItemRefeicaoPacienteBinding;
import br.edu.pucgoias.app.util.IconeCheck;

/** Refeições do "Meu Plano Alimentar": tocar no card marca/desmarca a refeição como feita hoje. */
public class RefeicaoPacienteAdapter extends RecyclerView.Adapter<RefeicaoPacienteAdapter.ViewHolder> {

    /** Refeição do plano já preparada para exibir. */
    public static final class Item {
        public final String id;
        public final String titulo;
        public final String alimentos;
        public final boolean feita;

        public Item(String id, String titulo, String alimentos, boolean feita) {
            this.id = id;
            this.titulo = titulo;
            this.alimentos = alimentos;
            this.feita = feita;
        }
    }

    public interface AoAlternar {
        void alternar(Item item);
    }

    private final List<Item> itens = new ArrayList<>();
    private final AoAlternar acao;
    private boolean habilitado;

    public RefeicaoPacienteAdapter(AoAlternar acao) {
        this.acao = acao;
    }

    /** Troca a lista exibida; habilitado = false enquanto o servidor responde. */
    public void atualizar(List<Item> novos, boolean podeAlternar) {
        itens.clear();
        itens.addAll(novos);
        habilitado = podeAlternar;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRefeicaoPacienteBinding binding = ItemRefeicaoPacienteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        View.OnClickListener alternar = v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao == RecyclerView.NO_POSITION || !habilitado) return;
            acao.alternar(itens.get(posicao));
        };
        binding.getRoot().setOnClickListener(alternar);
        binding.ivCheck.setOnClickListener(alternar);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = itens.get(position);
        holder.binding.getRoot().setTag("planned-" + item.id);
        holder.binding.tvHorario.setText(item.titulo);
        holder.binding.tvAlimentos.setText(item.alimentos);
        holder.binding.tvAlimentos.setVisibility(item.alimentos.isEmpty() ? View.GONE : View.VISIBLE);
        IconeCheck.atualizar(holder.binding.ivCheck, item.feita);
        holder.binding.ivCheck.setContentDescription(holder.itemView.getContext().getString(
                item.feita ? R.string.desmarcar_realizacao : R.string.marcar_realizada));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRefeicaoPacienteBinding binding;

        ViewHolder(ItemRefeicaoPacienteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
