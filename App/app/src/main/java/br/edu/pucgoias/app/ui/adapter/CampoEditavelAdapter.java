package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.pucgoias.app.databinding.ItemAlimentoEditavelBinding;
import br.edu.pucgoias.app.ui.nutricionista.edicao.CampoTexto;
import br.edu.pucgoias.app.util.TextoAlterado;

/**
 * Linhas editáveis com lixeira: alimentos de uma refeição (Editar Plano)
 * e itens da lista de compras (Editar Lista de Compras).
 */
public class CampoEditavelAdapter extends RecyclerView.Adapter<CampoEditavelAdapter.ViewHolder> {

    /** Avisa quem usa o adapter que uma linha foi removida pela lixeira. */
    public interface AoRemover {
        void removido(CampoTexto campo);
    }

    private List<? extends CampoTexto> campos;
    @StringRes private final int hint;
    @Nullable private final AoRemover aoRemover;
    @Nullable private Runnable aoAlterar;
    private boolean habilitado = true;

    public CampoEditavelAdapter(List<? extends CampoTexto> campos, @StringRes int hint) {
        this(campos, hint, null);
    }

    public CampoEditavelAdapter(List<? extends CampoTexto> campos, @StringRes int hint,
                                @Nullable AoRemover aoRemover) {
        this.campos = campos;
        this.hint = hint;
        this.aoRemover = aoRemover;
    }

    /** Troca a lista exibida (o card de refeição reciclado passa a mostrar outra refeição). */
    public void setCampos(List<? extends CampoTexto> novos) {
        campos = novos;
        notifyDataSetChanged();
    }

    /** Chamado sempre que o usuário digita ou apaga uma linha (o card atualiza o botão de salvar). */
    public void setAoAlterar(@Nullable Runnable acao) {
        aoAlterar = acao;
    }

    /** Bloqueia a edição enquanto o card está sendo salvo. */
    public void setHabilitado(boolean valor) {
        if (habilitado == valor) return;
        habilitado = valor;
        notifyItemRangeChanged(0, campos.size(), PAYLOAD_HABILITADO);
    }

    private static final Object PAYLOAD_HABILITADO = new Object();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlimentoEditavelBinding binding = ItemAlimentoEditavelBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        binding.etAlimento.setHint(hint);
        ViewHolder holder = new ViewHolder(binding);
        // Um único TextWatcher por ViewHolder: escreve sempre no campo que está ligado a ele agora.
        binding.etAlimento.addTextChangedListener(new TextoAlterado(texto -> {
            if (holder.campo == null) return; // setText do bind, não é digitação
            holder.campo.texto = texto;
            if (aoAlterar != null) aoAlterar.run();
        }));
        binding.btnRemoverAlimento.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao == RecyclerView.NO_POSITION || !habilitado) return;
            CampoTexto removido = campos.remove(posicao);
            notifyItemRemoved(posicao);
            if (aoRemover != null) aoRemover.removido(removido);
            if (aoAlterar != null) aoAlterar.run();
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_HABILITADO)) {
            aplicarHabilitado(holder);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.campo = null; // evita que o setText abaixo grave no campo anterior
        holder.binding.etAlimento.setText(campos.get(position).texto);
        holder.campo = campos.get(position);
        aplicarHabilitado(holder);
    }

    private void aplicarHabilitado(ViewHolder holder) {
        holder.binding.etAlimento.setEnabled(habilitado);
        holder.binding.btnRemoverAlimento.setEnabled(habilitado);
    }

    @Override
    public int getItemCount() {
        return campos.size();
    }

    /** Foca o campo da posição informada (usado logo depois de adicionar uma linha nova). */
    public static void focarCampo(RecyclerView lista, int posicao) {
        lista.post(() -> {
            RecyclerView.ViewHolder vh = lista.findViewHolderForAdapterPosition(posicao);
            if (vh instanceof ViewHolder) ((ViewHolder) vh).binding.etAlimento.requestFocus();
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ItemAlimentoEditavelBinding binding;
        CampoTexto campo;

        ViewHolder(ItemAlimentoEditavelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
