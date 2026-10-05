package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import br.edu.pucgoias.app.databinding.ItemTituloSecaoBinding;

/** Um único título de seção, usado junto com outros adapters num ConcatAdapter. */
public class TituloSecaoAdapter extends RecyclerView.Adapter<TituloSecaoAdapter.ViewHolder> {

    @StringRes private final int titulo;

    public TituloSecaoAdapter(@StringRes int titulo) {
        this.titulo = titulo;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemTituloSecaoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.binding.tvTituloSecao.setText(titulo);
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemTituloSecaoBinding binding;

        ViewHolder(ItemTituloSecaoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
