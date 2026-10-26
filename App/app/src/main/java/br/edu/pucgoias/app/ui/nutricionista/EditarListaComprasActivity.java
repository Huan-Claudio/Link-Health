package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.UUID;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.databinding.ActivityListaSimplesBinding;
import br.edu.pucgoias.app.ui.adapter.ShoppingEditorAdapter;
import br.edu.pucgoias.app.ui.nutricionista.edicao.ShoppingDraft;
import br.edu.pucgoias.app.ui.nutricionista.edicao.ShoppingEditorViewModel;
import br.edu.pucgoias.app.util.PatientNavigation;

/** Paciente ou nutricionista vinculado editam a mesma lista persistida. */
public class EditarListaComprasActivity extends BaseActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sessaoExpirada()) return;
        UUID patient = PatientNavigation.patientId(getIntent());
        if (patient == null) { toast(R.string.sessao_remota_necessaria); finish(); return; }
        ActivityListaSimplesBinding b = ActivityListaSimplesBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        configurarHeader(b.header, getString(R.string.lista_compras_de, PatientNavigation.name(getIntent())));
        ShoppingEditorViewModel model = new ViewModelProvider(this).get(ShoppingEditorViewModel.class);
        ShoppingEditorAdapter adapter = new ShoppingEditorAdapter(model.items, new ShoppingEditorAdapter.Actions() {
            public void save(ShoppingDraft item) { model.save(item); }
            public void remove(ShoppingDraft item) {
                if (item.pending) return;
                if (item.id == null) { model.remove(item); return; }
                registrarDialog(new MaterialAlertDialogBuilder(EditarListaComprasActivity.this)
                        .setTitle("Remover item?").setMessage(item.name)
                        .setNegativeButton(R.string.cancelar, null)
                        .setPositiveButton(R.string.remover, (d, w) -> model.remove(item)).show());
            }
        });
        b.lista.setLayoutManager(new LinearLayoutManager(this)); b.lista.setAdapter(adapter);
        b.btnRodape.setVisibility(View.VISIBLE);
        b.btnRodape.setOnClickListener(v -> {
            if (!model.loaded) model.load(patient, Sessao.getAccessToken());
            else { model.add(); b.lista.smoothScrollToPosition(model.items.size() - 1); }
        });
        model.changes().observe(this, ignored -> {
            adapter.notifyDataSetChanged();
            b.btnRodape.setEnabled(!model.loading);
            b.btnRodape.setText(model.loading ? "Carregando…" : model.loaded ? "Adicionar item" : "Tentar novamente");
            String message = model.takeMessage(); if (message != null) toast(message);
        });
        model.load(patient, Sessao.getAccessToken());
    }
}
