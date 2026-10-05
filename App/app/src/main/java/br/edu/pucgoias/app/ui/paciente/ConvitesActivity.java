package br.edu.pucgoias.app.ui.paciente;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityConvitesBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.adapter.ConviteAdapter;

/** Figma: "Dietitian dashboard" (paciente) – convites de nutricionistas. */
public class ConvitesActivity extends BaseActivity {

    private ActivityConvitesBinding binding;
    private List<String> convites;
    private ConviteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        binding = ActivityConvitesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String nome = Sessao.getNomeUsuario();
        binding.headerSaudacao.tvAvatar.setText(Paciente.iniciais(nome));
        binding.headerSaudacao.tvOla.setText(getString(R.string.ola_nome, nome));
        binding.headerSaudacao.btnVoltar.setOnClickListener(v -> finish());

        convites = MockData.getConvites();
        adapter = new ConviteAdapter(convites, this::aceitar);
        binding.listaConvites.setLayoutManager(new LinearLayoutManager(this));
        binding.listaConvites.setAdapter(adapter);
        atualizarVazio();
    }

    private void aceitar(String nutricionista, int posicao) {
        // TODO: registrar o aceite do convite no back-end.
        convites.remove(posicao);
        adapter.notifyItemRemoved(posicao);
        toast(getString(R.string.convite_aceito, nutricionista));
        atualizarVazio();
    }

    private void atualizarVazio() {
        binding.tvVazio.setVisibility(convites.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
