package br.edu.pucgoias.app.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.databinding.ItemConviteBinding;
import br.edu.pucgoias.app.model.Paciente;

/** Resultados da busca de paciente (tela "Novo paciente"), com o botão de enviar convite. */
public class BuscaPacienteAdapter extends RecyclerView.Adapter<BuscaPacienteAdapter.ViewHolder> {

    public interface OnConvidar {
        void onConvidar(Paciente paciente, int posicao);
    }

    private final List<Paciente> resultados = new ArrayList<>();
    /** CPFs que já receberam convite (lista guardada no ViewModel da tela). */
    private final List<String> convidados;
    private final OnConvidar onConvidar;

    public BuscaPacienteAdapter(List<String> convidados, OnConvidar onConvidar) {
        this.convidados = convidados;
        this.onConvidar = onConvidar;
    }

    public void atualizar(List<Paciente> novos) {
        resultados.clear();
        resultados.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemConviteBinding binding = ItemConviteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        binding.btnAdicionar.setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) onConvidar.onConvidar(resultados.get(posicao), posicao);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Paciente p = resultados.get(position);
        Context context = holder.itemView.getContext();
        holder.binding.tvAvatar.setBackgroundResource(R.drawable.bg_circle_primary);
        holder.binding.tvAvatar.setText(p.getIniciais());

        boolean jaVinculado = MockData.getPacientes().contains(p);
        boolean jaConvidado = convidados.contains(p.getCpf());
        // Já é paciente deste nutricionista ou já recebeu convite: o botão fica desativado.
        holder.binding.tvNome.setText(jaVinculado
                ? context.getString(R.string.paciente_ja_vinculado, p.getNome())
                : p.getNome());
        boolean podeConvidar = !jaVinculado && !jaConvidado;
        holder.binding.btnAdicionar.setEnabled(podeConvidar);
        holder.binding.btnAdicionar.setAlpha(podeConvidar ? 1f : 0.4f);
    }

    @Override
    public int getItemCount() {
        return resultados.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemConviteBinding binding;

        ViewHolder(ItemConviteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
