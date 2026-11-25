package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ItemRefeicaoEditavelBinding;
import br.edu.pucgoias.app.ui.nutricionista.edicao.AlimentoRascunho;
import br.edu.pucgoias.app.ui.nutricionista.edicao.CampoTexto;
import br.edu.pucgoias.app.ui.nutricionista.edicao.RascunhoRefeicao;

/**
 * Cards de refeição da tela "Editar Plano Alimentar"; cada card tem uma lista interna de alimentos.
 * As listas internas não compartilham RecycledViewPool: cada linha de alimento pertence ao adapter
 * do seu card (a lixeira remove da refeição certa).
 */
public class RefeicaoEditavelAdapter extends RecyclerView.Adapter<RefeicaoEditavelAdapter.ViewHolder> {

    /** Ações que dependem da Activity (diálogos e servidor). */
    public interface Acoes {
        void escolherTipo(RascunhoRefeicao refeicao, View ancora);
        void escolherHorario(RascunhoRefeicao refeicao);
        void remover(RascunhoRefeicao refeicao);
        void salvar(RascunhoRefeicao refeicao);
    }

    /** Atualiza só o estado dos botões (sem reescrever os textos que o usuário está digitando). */
    private static final Object PAYLOAD_ESTADO = new Object();

    private final List<RascunhoRefeicao> refeicoes;
    private final Acoes acoes;
    private boolean ocupado;

    public RefeicaoEditavelAdapter(List<RascunhoRefeicao> refeicoes, Acoes acoes) {
        this.refeicoes = refeicoes;
        this.acoes = acoes;
    }

    /** Enquanto o servidor responde, os botões ficam desabilitados. */
    public void setOcupado(boolean valor) {
        ocupado = valor;
        notifyItemRangeChanged(0, refeicoes.size(), PAYLOAD_ESTADO);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemRefeicaoEditavelBinding binding = ItemRefeicaoEditavelBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        // Digitar ou apagar um alimento troca o botão entre "Salvo" e "Salvar Alterações".
        holder.alimentosAdapter.setAoAlterar(() -> {
            if (holder.atual != null) atualizarBotaoSalvar(holder, holder.atual);
        });
        binding.listaAlimentos.setLayoutManager(new LinearLayoutManager(parent.getContext()));
        binding.listaAlimentos.setAdapter(holder.alimentosAdapter);

        binding.tvTipoRefeicao.setOnClickListener(v -> executar(holder, r -> acoes.escolherTipo(r, v)));
        binding.tvHorario.setOnClickListener(v -> executar(holder, acoes::escolherHorario));
        binding.btnRemoverRefeicao.setOnClickListener(v -> executar(holder, acoes::remover));
        binding.btnSalvarRefeicao.getRoot().setOnClickListener(v -> executar(holder, acoes::salvar));
        binding.btnAdicionarAlimento.getRoot().setOnClickListener(v -> executar(holder, r -> {
            r.alimentos.add(new AlimentoRascunho(null, ""));
            int nova = r.alimentos.size() - 1;
            holder.alimentosAdapter.notifyItemInserted(nova);
            CampoEditavelAdapter.focarCampo(binding.listaAlimentos, nova);
        }));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_ESTADO)) {
            aplicarEstado(holder, refeicoes.get(position));
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RascunhoRefeicao r = refeicoes.get(position);
        holder.atual = r;
        holder.binding.tvTipoRefeicao.setText(r.nome);
        holder.binding.tvHorario.setText(r.horario);
        holder.alimentosAdapter.setCampos(r.alimentos);
        aplicarEstado(holder, r);
    }

    private void aplicarEstado(ViewHolder holder, RascunhoRefeicao r) {
        boolean livre = !ocupado && !r.salvando;
        holder.binding.tvTipoRefeicao.setEnabled(livre);
        holder.binding.tvHorario.setEnabled(livre);
        holder.binding.btnRemoverRefeicao.setEnabled(livre);
        holder.binding.btnAdicionarAlimento.getRoot().setEnabled(livre);
        holder.alimentosAdapter.setHabilitado(!r.salvando);
        atualizarBotaoSalvar(holder, r);
    }

    /**
     * "Salvando…" durante o envio, "Salvar Alterações" quando há algo a salvar
     * e "Salvo" (desabilitado) quando o card está igual ao que está gravado.
     */
    private void atualizarBotaoSalvar(ViewHolder holder, RascunhoRefeicao r) {
        TextView salvar = holder.binding.btnSalvarRefeicao.getRoot();
        boolean alterado = r.isAlterado();
        if (r.salvando) salvar.setText(R.string.salvando);
        else salvar.setText(alterado ? R.string.salvar_alteracoes : R.string.salvo);
        salvar.setEnabled(!ocupado && !r.salvando && alterado);
    }

    @Override
    public int getItemCount() {
        return refeicoes.size();
    }

    private interface AcaoNoCard {
        void executar(RascunhoRefeicao refeicao);
    }

    /** Usa a refeição ligada ao card agora (a posição pode ter mudado depois de remover outro card). */
    private void executar(ViewHolder holder, AcaoNoCard acao) {
        int posicao = holder.getBindingAdapterPosition();
        if (posicao == RecyclerView.NO_POSITION || posicao >= refeicoes.size()) return;
        acao.executar(refeicoes.get(posicao));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemRefeicaoEditavelBinding binding;
        final CampoEditavelAdapter alimentosAdapter;
        @Nullable RascunhoRefeicao atual;

        ViewHolder(ItemRefeicaoEditavelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.alimentosAdapter = new CampoEditavelAdapter(new ArrayList<CampoTexto>(),
                    R.string.hint_alimento, campo -> {
                        if (atual != null) atual.alimentoRemovido(campo);
                    });
        }
    }
}
