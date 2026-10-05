package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ItemRefeicaoEditavelBinding;
import br.edu.pucgoias.app.ui.nutricionista.edicao.CampoTexto;
import br.edu.pucgoias.app.ui.nutricionista.edicao.RascunhoRefeicao;

/**
 * Cards de refeição da tela "Editar Plano Alimentar"; cada card tem uma lista interna de alimentos.
 * As listas internas não compartilham RecycledViewPool: cada linha de alimento pertence ao adapter
 * do seu card (a lixeira remove da refeição certa).
 */
public class RefeicaoEditavelAdapter extends RecyclerView.Adapter<RefeicaoEditavelAdapter.ViewHolder> {

    /** Ações que dependem da Activity (diálogos e acesso ao paciente). */
    public interface Acoes {
        void escolherTipo(int posicao, View ancora);
        void escolherHorario(int posicao);
        void remover(int posicao);
        void salvar(int posicao);
    }

    private final List<RascunhoRefeicao> refeicoes;
    private final Acoes acoes;

    public RefeicaoEditavelAdapter(List<RascunhoRefeicao> refeicoes, Acoes acoes) {
        this.refeicoes = refeicoes;
        this.acoes = acoes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRefeicaoEditavelBinding binding = ItemRefeicaoEditavelBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        binding.listaAlimentos.setLayoutManager(new LinearLayoutManager(parent.getContext()));
        binding.listaAlimentos.setAdapter(holder.alimentosAdapter);

        binding.tvTipoRefeicao.setOnClickListener(v -> executar(holder, p -> acoes.escolherTipo(p, v)));
        binding.tvHorario.setOnClickListener(v -> executar(holder, acoes::escolherHorario));
        binding.btnRemoverRefeicao.setOnClickListener(v -> executar(holder, acoes::remover));
        binding.btnSalvarRefeicao.getRoot().setOnClickListener(v -> executar(holder, acoes::salvar));
        binding.btnAdicionarAlimento.getRoot().setOnClickListener(v -> executar(holder, p -> {
            List<CampoTexto> alimentos = refeicoes.get(p).alimentos;
            alimentos.add(new CampoTexto(""));
            int nova = alimentos.size() - 1;
            holder.alimentosAdapter.notifyItemInserted(nova);
            CampoEditavelAdapter.focarCampo(binding.listaAlimentos, nova);
        }));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RascunhoRefeicao r = refeicoes.get(position);
        holder.binding.tvTipoRefeicao.setText(r.nome);
        holder.binding.tvHorario.setText(r.horario);
        holder.alimentosAdapter.setCampos(r.alimentos);
    }

    @Override
    public int getItemCount() {
        return refeicoes.size();
    }

    private interface AcaoNaPosicao {
        void executar(int posicao);
    }

    /** Usa a posição atual do card (pode ter mudado depois de remover outro card). */
    private static void executar(ViewHolder holder, AcaoNaPosicao acao) {
        int posicao = holder.getBindingAdapterPosition();
        if (posicao != RecyclerView.NO_POSITION) acao.executar(posicao);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRefeicaoEditavelBinding binding;
        final CampoEditavelAdapter alimentosAdapter;

        ViewHolder(ItemRefeicaoEditavelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.alimentosAdapter = new CampoEditavelAdapter(
                    new ArrayList<CampoTexto>(), R.string.hint_alimento);
        }
    }
}
