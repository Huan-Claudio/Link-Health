package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import br.edu.pucgoias.app.databinding.ViewBotaoAdicionarRefeicaoBinding;

/** Botão "+ Adicionar Refeição" como último item da lista (ConcatAdapter), logo abaixo dos cards. */
public class BotaoAdicionarRefeicaoAdapter extends RecyclerView.Adapter<BotaoAdicionarRefeicaoAdapter.ViewHolder> {

    private final View.OnClickListener onClick;

    public BotaoAdicionarRefeicaoAdapter(View.OnClickListener onClick) {
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewBotaoAdicionarRefeicaoBinding binding = ViewBotaoAdicionarRefeicaoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        binding.getRoot().setOnClickListener(onClick);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // conteúdo fixo
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(ViewBotaoAdicionarRefeicaoBinding binding) {
            super(binding.getRoot());
        }
    }
}
