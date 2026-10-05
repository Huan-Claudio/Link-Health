package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.pucgoias.app.databinding.ItemRefeicaoPacienteBinding;
import br.edu.pucgoias.app.model.Refeicao;
import br.edu.pucgoias.app.util.IconeCheck;

/** Refeições do "Meu Plano Alimentar": tocar no card marca/desmarca a refeição como feita. */
public class RefeicaoPacienteAdapter extends RecyclerView.Adapter<RefeicaoPacienteAdapter.ViewHolder> {

    private final List<Refeicao> refeicoes;

    public RefeicaoPacienteAdapter(List<Refeicao> refeicoes) {
        this.refeicoes = refeicoes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRefeicaoPacienteBinding binding = ItemRefeicaoPacienteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        View.OnClickListener alternar = v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao == RecyclerView.NO_POSITION) return;
            Refeicao refeicao = refeicoes.get(posicao);
            refeicao.setConcluida(!refeicao.isConcluida());
            notifyItemChanged(posicao);
        };
        binding.getRoot().setOnClickListener(alternar);
        binding.ivCheck.setOnClickListener(alternar);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Refeicao refeicao = refeicoes.get(position);
        holder.binding.tvHorario.setText(refeicao.getTitulo());
        StringBuilder alimentos = new StringBuilder();
        for (String a : refeicao.getAlimentos()) {
            if (alimentos.length() > 0) alimentos.append('\n');
            alimentos.append("• ").append(a);
        }
        holder.binding.tvAlimentos.setText(alimentos);
        IconeCheck.atualizar(holder.binding.ivCheck, refeicao.isConcluida());
    }

    @Override
    public int getItemCount() {
        return refeicoes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRefeicaoPacienteBinding binding;

        ViewHolder(ItemRefeicaoPacienteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
