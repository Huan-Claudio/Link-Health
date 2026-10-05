package br.edu.pucgoias.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ItemCompraBinding;
import br.edu.pucgoias.app.databinding.ItemRegistroDiaBinding;
import br.edu.pucgoias.app.model.RegistroDia;
import br.edu.pucgoias.app.util.Formatador;

/** Dias do "Registro diário do paciente": água do dia e refeições feitas/não feitas (lista interna). */
public class RegistroDiaAdapter extends RecyclerView.Adapter<RegistroDiaAdapter.ViewHolder> {

    private final List<RegistroDia> dias;
    /** As listas internas de refeições compartilham as views recicladas. */
    private final RecyclerView.RecycledViewPool poolRefeicoes = new RecyclerView.RecycledViewPool();

    public RegistroDiaAdapter(List<RegistroDia> dias) {
        this.dias = dias;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRegistroDiaBinding binding = ItemRegistroDiaBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        binding.listaRefeicoes.setLayoutManager(new LinearLayoutManager(parent.getContext()));
        binding.listaRefeicoes.setRecycledViewPool(poolRefeicoes);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RegistroDia dia = dias.get(position);
        Context context = holder.itemView.getContext();
        holder.binding.tvData.setText(dia.getData());
        holder.binding.progressAgua.setProgressCompat(
                Formatador.porcentagem(dia.getAguaConsumida(), dia.getMetaAgua()), false);
        holder.binding.tvAgua.setText(context.getString(R.string.agua_valor,
                Formatador.litros(dia.getAguaConsumida()), Formatador.litros(dia.getMetaAgua())));
        holder.binding.listaRefeicoes.setAdapter(
                new RefeicaoStatusAdapter(new ArrayList<>(dia.getRefeicoes().entrySet())));
    }

    @Override
    public int getItemCount() {
        return dias.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRegistroDiaBinding binding;

        ViewHolder(ItemRegistroDiaBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /** Refeição do dia com ícone verde (feita) ou vermelho (não feita). */
    static class RefeicaoStatusAdapter extends RecyclerView.Adapter<RefeicaoStatusAdapter.ViewHolder> {

        private final List<Map.Entry<String, Boolean>> refeicoes;

        RefeicaoStatusAdapter(List<Map.Entry<String, Boolean>> refeicoes) {
            this.refeicoes = refeicoes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemCompraBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map.Entry<String, Boolean> refeicao = refeicoes.get(position);
            Context context = holder.itemView.getContext();
            boolean feita = Boolean.TRUE.equals(refeicao.getValue());
            holder.binding.tvDescricao.setText(refeicao.getKey());
            holder.binding.ivAcao.setImageResource(feita ? R.drawable.ic_check_circle : R.drawable.ic_block);
            holder.binding.ivAcao.setImageTintList(
                    context.getColorStateList(feita ? R.color.lh_green : R.color.lh_red));
        }

        @Override
        public int getItemCount() {
            return refeicoes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemCompraBinding binding;

            ViewHolder(ItemCompraBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
