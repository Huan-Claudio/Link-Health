package br.edu.pucgoias.app.ui.nutricionista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.MockData;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityHomeNutricionistaBinding;
import br.edu.pucgoias.app.model.Paciente;
import br.edu.pucgoias.app.ui.adapter.PacienteAdapter;
import br.edu.pucgoias.app.ui.comum.ConfiguracoesActivity;
import br.edu.pucgoias.app.util.Navegacao;
import br.edu.pucgoias.app.util.TextoAlterado;

/** Figma: "Home Nutricionista" – lista de pacientes (RecyclerView) com busca. */
public class HomeNutricionistaActivity extends BaseActivity {

    private ActivityHomeNutricionistaBinding binding;
    private PacienteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        binding = ActivityHomeNutricionistaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvAvatar.setText("DR.");
        binding.layoutPerfil.setOnClickListener(v ->
                startActivity(new Intent(this, ConfiguracoesActivity.class)));
        binding.btnNovoPaciente.setOnClickListener(v ->
                startActivity(new Intent(this, BuscarPacienteActivity.class)));

        adapter = new PacienteAdapter(paciente -> startActivity(
                Navegacao.comPaciente(this, FichaPacienteActivity.class, paciente)));
        binding.listaPacientes.setLayoutManager(new LinearLayoutManager(this));
        binding.listaPacientes.setHasFixedSize(true);
        binding.listaPacientes.setAdapter(adapter);

        binding.etBusca.addTextChangedListener(new TextoAlterado(texto -> filtrar()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega ao voltar: o nome ou a lista de pacientes podem ter mudado em outra tela.
        binding.tvOla.setText(getString(R.string.ola_nome, Sessao.getNomeUsuario()));
        binding.tvPacientesAtivos.setText(getString(R.string.pacientes_ativos, MockData.getPacientes().size()));
        filtrar();
    }

    private void filtrar() {
        String filtro = normalizar(binding.etBusca.getText().toString());
        List<Paciente> exibidos = new ArrayList<>();
        for (Paciente p : MockData.getPacientes()) {
            if (filtro.isEmpty() || normalizar(p.getNome()).contains(filtro)) exibidos.add(p);
        }
        adapter.atualizar(exibidos);
        binding.tvVazio.setVisibility(exibidos.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /** Remove acentos e deixa minúsculo para a busca ("José" encontra "jose"). */
    static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }
}
