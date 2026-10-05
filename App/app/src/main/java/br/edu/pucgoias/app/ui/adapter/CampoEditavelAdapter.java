package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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

    private List<? extends CampoTexto> campos;
    @StringRes private final int hint;

    public CampoEditavelAdapter(List<? extends CampoTexto> campos, @StringRes int hint) {
        this.campos = campos;
        this.hint = hint;
    }

    /** Troca a lista exibida (o card de refeição reciclado passa a mostrar outra refeição). */
    public void setCampos(List<? extends CampoTexto> novos) {
        campos = novos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlimentoEditavelBinding binding = ItemAlimentoEditavelBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        binding.etAlimento.setHint(hint);
        ViewHolder holder = new ViewHolder(binding);
        // Um único TextWatcher por ViewHolder: escreve sempre no campo que está ligado a ele agora.
        binding.etAlimento.addTextChangedListener(new TextoAlterado(texto -> {
            if (holder.campo != null) holder.campo.texto = texto;
        }));
        binding.btnRemoverAlimento.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao == RecyclerView.NO_POSITION) return;
            campos.remove(posicao);
            notifyItemRemoved(posicao);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.campo = null; // evita que o setText abaixo grave no campo anterior
        holder.binding.etAlimento.setText(campos.get(position).texto);
        holder.campo = campos.get(position);
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
