package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import br.edu.pucgoias.app.databinding.ItemPlanoStatusBinding;

/**
 * Aviso no topo da tela "Editar Plano Alimentar" (primeiro item do ConcatAdapter).
 * Só aparece enquanto o plano carrega ou quando o servidor devolve um erro; no resto do tempo
 * a lista não tem este item.
 */
public class PlanoStatusAdapter extends RecyclerView.Adapter<PlanoStatusAdapter.ViewHolder> {

    private final Runnable aoTocar;
    private String titulo = "";
    private String mensagem = "";
    private boolean visivel;

    /** @param aoTocar toque no aviso (usado para tentar de novo quando há erro) */
    public PlanoStatusAdapter(Runnable aoTocar) {
        this.aoTocar = aoTocar;
    }

    public void mostrar(String novoTitulo, String novaMensagem) {
        titulo = novoTitulo;
        mensagem = novaMensagem;
        if (visivel) {
            notifyItemChanged(0);
        } else {
            visivel = true;
            notifyItemInserted(0);
        }
    }

    public void ocultar() {
        if (!visivel) return;
        visivel = false;
        notifyItemRemoved(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPlanoStatusBinding binding = ItemPlanoStatusBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        binding.getRoot().setOnClickListener(v -> aoTocar.run());
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.binding.tvStatusTitulo.setText(titulo);
        holder.binding.tvStatusMensagem.setText(mensagem);
    }

    @Override
    public int getItemCount() {
        return visivel ? 1 : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPlanoStatusBinding binding;

        ViewHolder(ItemPlanoStatusBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
