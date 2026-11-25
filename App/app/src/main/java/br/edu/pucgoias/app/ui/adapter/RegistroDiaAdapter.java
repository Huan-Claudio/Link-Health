package br.edu.pucgoias.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ItemRefeicaoDiaBinding;
import br.edu.pucgoias.app.databinding.ItemRegistroDiaBinding;
import br.edu.pucgoias.app.util.Formatador;

/**
 * Dias do "Registro diário do paciente": água do dia, refeições feitas/não feitas (lista interna)
 * e anotações do dia.
 */
public class RegistroDiaAdapter extends RecyclerView.Adapter<RegistroDiaAdapter.ViewHolder> {

    /** Refeição do plano exibida no card do dia, com os alimentos da refeição. */
    public static final class Refeicao {
        public final String nome;
        public final boolean feita;
        public final List<String> alimentos;

        public Refeicao(String nome, boolean feita, List<String> alimentos) {
            this.nome = nome;
            this.feita = feita;
            this.alimentos = alimentos;
        }
    }

    /** Um dia do histórico, já montado pela tela a partir dos registros do servidor. */
    public static final class Dia {
        public final String data;
        public final long aguaMl;
        @Nullable public final Integer metaMl;
        public final List<Refeicao> refeicoes;
        public final String anotacoes;

        public Dia(String data, long aguaMl, @Nullable Integer metaMl, List<Refeicao> refeicoes,
                   String anotacoes) {
            this.data = data;
            this.aguaMl = aguaMl;
            this.metaMl = metaMl;
            this.refeicoes = refeicoes;
            this.anotacoes = anotacoes;
        }
    }

    private final List<Dia> dias;
    /** As listas internas de refeições compartilham as views recicladas (são só leitura). */
    private final RecyclerView.RecycledViewPool poolRefeicoes = new RecyclerView.RecycledViewPool();

    public RegistroDiaAdapter(List<Dia> dias) {
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
        Dia dia = dias.get(position);
        Context context = holder.itemView.getContext();
        holder.binding.tvData.setText(dia.data);

        String consumido = Formatador.litros(dia.aguaMl / 1000.0);
        if (dia.metaMl != null && dia.metaMl > 0) {
            holder.binding.progressAgua.setProgressCompat(
                    Formatador.porcentagem(dia.aguaMl, dia.metaMl), false);
            holder.binding.tvAgua.setText(context.getString(R.string.agua_valor,
                    consumido, Formatador.litros(dia.metaMl / 1000.0)));
        } else {
            holder.binding.progressAgua.setProgressCompat(0, false);
            holder.binding.tvAgua.setText(context.getString(R.string.agua_sem_meta, consumido));
        }

        holder.binding.listaRefeicoes.setAdapter(new RefeicaoStatusAdapter(dia.refeicoes));
        holder.binding.tvAnotacoes.setText(dia.anotacoes.isEmpty()
                ? context.getString(R.string.sem_anotacoes)
                : dia.anotacoes);
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

    /**
     * Refeição do dia com ícone verde (feita) ou vermelho (não feita); embaixo do nome, os alimentos
     * da refeição no plano (feita ou não).
     */
    static class RefeicaoStatusAdapter extends RecyclerView.Adapter<RefeicaoStatusAdapter.ViewHolder> {

        private final List<Refeicao> refeicoes;

        RefeicaoStatusAdapter(List<Refeicao> refeicoes) {
            this.refeicoes = refeicoes;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(ItemRefeicaoDiaBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Refeicao refeicao = refeicoes.get(position);
            Context context = holder.itemView.getContext();
            holder.binding.tvNomeRefeicao.setText(refeicao.nome);
            holder.binding.ivStatus.setImageResource(
                    refeicao.feita ? R.drawable.ic_check_circle : R.drawable.ic_block);
            holder.binding.ivStatus.setImageTintList(
                    context.getColorStateList(refeicao.feita ? R.color.lh_green : R.color.lh_red));
            holder.binding.ivStatus.setContentDescription(context.getString(
                    refeicao.feita ? R.string.refeicao_feita : R.string.refeicao_nao_feita));

            StringBuilder alimentos = new StringBuilder();
            for (String alimento : refeicao.alimentos) {
                if (alimento.trim().isEmpty()) continue;
                if (alimentos.length() > 0) alimentos.append('\n');
                alimentos.append("• ").append(alimento.trim());
            }
            holder.binding.tvAlimentosRefeicao.setText(alimentos);
            holder.binding.tvAlimentosRefeicao.setVisibility(
                    alimentos.length() == 0 ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return refeicoes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final ItemRefeicaoDiaBinding binding;

            ViewHolder(ItemRefeicaoDiaBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
