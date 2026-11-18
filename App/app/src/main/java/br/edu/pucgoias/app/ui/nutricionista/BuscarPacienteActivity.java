package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.databinding.ActivityBuscarPacienteBinding;
import br.edu.pucgoias.app.ui.adapter.BuscaPacienteAdapter;
import br.edu.pucgoias.app.ui.comum.FollowUpsViewModel;
import br.edu.pucgoias.app.util.TextoAlterado;

public class BuscarPacienteActivity extends BaseActivity {
    private ActivityBuscarPacienteBinding binding;
    private FollowUpsViewModel model;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        if (!Sessao.temSessaoRemota() || Sessao.getPerfil() != Perfil.NUTRICIONISTA) { finish(); return; }
        binding = ActivityBuscarPacienteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.headerSaudacao.tvOla.setText("Novo paciente");
        binding.headerSaudacao.btnVoltar.setOnClickListener(v -> finish());
        binding.etBusca.setHint("Buscar por nome, CPF ou e-mail");
        model = new ViewModelProvider(this).get(FollowUpsViewModel.class);
        BuscaPacienteAdapter adapter = new BuscaPacienteAdapter(model, id -> model.invite(id));
        binding.listaResultados.setLayoutManager(new LinearLayoutManager(this));
        binding.listaResultados.setAdapter(adapter);
        binding.etBusca.setText(model.query);
        binding.etBusca.addTextChangedListener(new TextoAlterado(model::search));
        binding.tvVazio.setOnClickListener(v -> { model.load(); model.search(binding.etBusca.getText().toString()); });
        model.changes.observe(this, ignored -> {
            adapter.notifyDataSetChanged();
            String message = model.error != null ? model.error + "\nToque para tentar novamente."
                    : model.pending ? "Enviando convite..." : model.loading || model.searching ? "Carregando..."
                    : model.query.isEmpty() ? "Digite o nome, CPF ou e-mail do paciente."
                    : model.results.length() == 0 ? "Nenhum paciente encontrado." : "";
            binding.tvVazio.setText(message);
            binding.tvVazio.setVisibility(message.isEmpty() ? View.GONE : View.VISIBLE);
        });
        if (!model.loaded) model.load();
    }
}
