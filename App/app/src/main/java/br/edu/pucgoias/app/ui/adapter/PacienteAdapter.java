package br.edu.pucgoias.app.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.edu.pucgoias.app.databinding.ItemPacienteBinding;
import br.edu.pucgoias.app.model.Paciente;

/** Lista "Meus Pacientes" da Home do nutricionista. */
public class PacienteAdapter extends RecyclerView.Adapter<PacienteAdapter.ViewHolder> {

    public interface OnPacienteClick {
        void onClick(Paciente paciente);
    }

    private final List<Paciente> pacientes = new ArrayList<>();
    private final OnPacienteClick onClick;

    public PacienteAdapter(OnPacienteClick onClick) {
        this.onClick = onClick;
    }

    /** Troca a lista exibida (ex.: depois de filtrar pela busca). */
    public void atualizar(List<Paciente> novos) {
        pacientes.clear();
        pacientes.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPacienteBinding binding = ItemPacienteBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        ViewHolder holder = new ViewHolder(binding);
        binding.getRoot().setOnClickListener(v -> {
            int posicao = holder.getBindingAdapterPosition();
            if (posicao != RecyclerView.NO_POSITION) onClick.onClick(pacientes.get(posicao));
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Paciente p = pacientes.get(position);
        holder.binding.tvAvatar.setText(p.getIniciais());
        holder.binding.tvNome.setText(p.getNome());
        holder.binding.tvObjetivo.setText(p.getObjetivo());
    }

    @Override
    public int getItemCount() {
        return pacientes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemPacienteBinding binding;

        ViewHolder(ItemPacienteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
