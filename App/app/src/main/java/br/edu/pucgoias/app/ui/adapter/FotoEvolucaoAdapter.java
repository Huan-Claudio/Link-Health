package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.databinding.ItemFotoEvolucaoBinding;
import br.edu.pucgoias.app.model.FotoEvolucao;
import br.edu.pucgoias.app.util.Formatador;

/** Grade de fotos da evolução corporal (GridLayoutManager com 2 colunas). */
public class FotoEvolucaoAdapter extends RecyclerView.Adapter<FotoEvolucaoAdapter.ViewHolder> {

    public interface OnAbrirFoto {
        void onAbrir(FotoEvolucao foto);
    }

    private final List<FotoEvolucao> fotos;
    private final OnAbrirFoto onAbrir;

    public FotoEvolucaoAdapter(List<FotoEvolucao> fotos, OnAbrirFoto onAbrir) {
        this.fotos = fotos;
        this.onAbrir = onAbrir;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFotoEvolucaoBinding binding = ItemFotoEvolucaoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        binding.btnAbrirImagem.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) onAbrir.onAbrir(fotos.get(posicao));
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FotoEvolucao foto = fotos.get(position);
        mostrarFoto(holder.binding.ivFoto, foto);
        holder.binding.tvData.setText(foto.getData());
        holder.binding.tvPeso.setText(holder.itemView.getContext()
                .getString(R.string.peso_valor, Formatador.numero(foto.getPeso())));
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    /** Mostra a foto tirada ou, se não houver, o ícone de imagem padrão (a view pode ter sido reciclada). */
    public static void mostrarFoto(ImageView iv, FotoEvolucao foto) {
        if (foto.getFoto() != null) {
            iv.setImageBitmap(foto.getFoto());
            iv.setImageTintList(null);
            iv.setPadding(0, 0, 0, 0);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setClipToOutline(true);
        } else {
            int padding = iv.getResources().getDimensionPixelSize(R.dimen.lh_space_l);
            iv.setImageResource(R.drawable.ic_image);
            iv.setImageTintList(iv.getContext().getColorStateList(R.color.lh_placeholder));
            iv.setPadding(padding, padding, padding, padding);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemFotoEvolucaoBinding binding;

        ViewHolder(ItemFotoEvolucaoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
