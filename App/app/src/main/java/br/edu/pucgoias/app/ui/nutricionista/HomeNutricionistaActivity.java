package br.edu.pucgoias.app.ui.nutricionista;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecommendedProductsApi;
import br.edu.pucgoias.app.databinding.ActivityHomeNutricionistaBinding;
import br.edu.pucgoias.app.ui.adapter.LinkedPatientAdapter;
import br.edu.pucgoias.app.ui.comum.ConfiguracoesActivity;
import br.edu.pucgoias.app.util.PatientNavigation;
import br.edu.pucgoias.app.util.TextoAlterado;

/** Entrada das funções conectadas, usando os acompanhamentos reais já existentes. */
public class HomeNutricionistaActivity extends BaseActivity {
    private ActivityHomeNutricionistaBinding binding;
    private LinkedPatientAdapter adapter;
    private final List<RecommendedProductsApi.Patient> patients = new ArrayList<>();
    private boolean loading;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        binding = ActivityHomeNutricionistaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.tvAvatar.setText("DR.");
        binding.layoutPerfil.setOnClickListener(v -> startActivity(new Intent(this, ConfiguracoesActivity.class)));
        binding.btnNovoPaciente.setOnClickListener(v -> startActivity(new Intent(this, BuscarPacienteActivity.class)));
        binding.btnProdutosReais.setOnClickListener(v -> escolherPacienteParaProdutos());
        adapter = new LinkedPatientAdapter(p -> startActivity(PatientNavigation.intent(
                this, FichaPacienteActivity.class, p.id, p.name)));
        binding.listaPacientes.setLayoutManager(new LinearLayoutManager(this));
        binding.listaPacientes.setAdapter(adapter);
        binding.etBusca.addTextChangedListener(new TextoAlterado(texto -> filtrar()));
        binding.tvVazio.setOnClickListener(v -> carregar());
    }

    @Override protected void onResume() {
        super.onResume();
        if (binding == null) return;
        binding.tvOla.setText(getString(R.string.ola_nome, Sessao.getNomeUsuario()));
        carregar();
    }

    private void carregar() {
        if (loading) return;
        if (!Sessao.temSessaoRemota()) {
            binding.tvVazio.setText(R.string.sessao_remota_necessaria);
            binding.tvVazio.setVisibility(View.VISIBLE);
            return;
        }
        loading = true;
        binding.btnProdutosReais.setEnabled(false);
        binding.tvVazio.setText("Carregando pacientes vinculados…");
        binding.tvVazio.setVisibility(View.VISIBLE);
        new RecommendedProductsApi().patients(Sessao.getAccessToken(), new RecommendedProductsApi.PatientCallback() {
            public void onSuccess(List<RecommendedProductsApi.Patient> result) { runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                loading = false; patients.clear(); patients.addAll(result);
                binding.btnProdutosReais.setEnabled(true);
                binding.tvPacientesAtivos.setText(getString(R.string.pacientes_ativos, patients.size()));
                filtrar();
            }); }
            public void onError(String message) { runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                loading = false; patients.clear(); adapter.update(patients);
                binding.btnProdutosReais.setEnabled(true);
                binding.tvPacientesAtivos.setText("Pacientes indisponíveis");
                binding.tvVazio.setText(message + "\nToque para tentar novamente.");
                binding.tvVazio.setVisibility(View.VISIBLE);
            }); }
        });
    }

    private void escolherPacienteParaProdutos() {
        if (loading) return;
        if (patients.isEmpty()) { carregar(); toast("Nenhum paciente com acompanhamento ativo."); return; }
        String[] names = new String[patients.size()];
        for (int i = 0; i < patients.size(); i++) names[i] = patients.get(i).name;
        registrarDialog(new MaterialAlertDialogBuilder(this).setTitle("Escolha o paciente vinculado")
                .setItems(names, (d, index) -> {
                    RecommendedProductsApi.Patient p = patients.get(index);
                    startActivity(PatientNavigation.intent(this, EditarProdutosActivity.class, p.id, p.name));
                }).setNegativeButton(R.string.cancelar, null).show());
    }

    private void filtrar() {
        if (loading) return;
        String filter = normalizar(binding.etBusca.getText().toString());
        List<RecommendedProductsApi.Patient> shown = new ArrayList<>();
        for (RecommendedProductsApi.Patient p : patients) if (filter.isEmpty() || normalizar(p.name).contains(filter)) shown.add(p);
        adapter.update(shown);
        binding.tvVazio.setText(patients.isEmpty() ? "Nenhum paciente com acompanhamento ativo. Toque para atualizar."
                : "Nenhum paciente encontrado nesta lista.");
        binding.tvVazio.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
    }

    static String normalizar(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
}
