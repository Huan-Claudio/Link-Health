package br.edu.pucgoias.app.ui.nutricionista;

import android.os.Bundle;

import android.widget.LinearLayout;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;
import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.comum.*;
import br.edu.pucgoias.app.util.PatientNavigation;

/** Histórico por dia e edição dos registros reais do paciente autenticado. */
public class RegistroDiarioActivity extends BaseActivity {
    private RecordsScreen screen;
    private RecordsViewModel vm;
    private boolean own;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if (sessaoExpirada()) return;
        UUID patient = PatientNavigation.patientId(getIntent());
        if (patient == null) { fecharPacienteNaoEncontrado(); return; }
        own = Sessao.getPerfil() == Perfil.PACIENTE;
        screen = new RecordsScreen(this, "Histórico dos registros"); setContentView(screen.root);
        vm = new ViewModelProvider(this).get(RecordsViewModel.class);
        if (own) {
            MaterialButton add = screen.button("Registrar consumo de água", v -> registrarDialog(RecordDialogs.edit(this, vm, RecordsApi.WATER, null)));
            add.setId(R.id.recordAddWater); screen.actions.addView(add);
        }
        screen.refresh.setOnClickListener(v -> vm.reload());
        vm.changes.observe(this, ignored -> render());
        vm.configure(patient, Sessao.getAccessToken(), false, RecordsApi.HISTORY, RecordsApi.WATER, RecordsApi.MEALS, RecordsApi.WEIGHT);
    }
    @Override protected void onResume() { super.onResume(); if (vm != null) vm.reload(); }
    private void render() {
        screen.state(vm); screen.rows.removeAllViews();
        JSONArray days = vm.items(RecordsApi.HISTORY);
        if (days.length() == 0 && vm.loading == 0) screen.card("Seu histórico", "Nenhum registro encontrado.");
        for (int i = 0; i < days.length(); i++) {
            JSONObject day = days.optJSONObject(i); if (day == null) continue;
            String weight = RecordFormatting.value(day, "weightKg");
            screen.card(RecordFormatting.date(day.optString("date")), day.optLong("waterMilliliters") + " ml de água · " + day.optInt("completedMeals") + " refeições · " + day.optInt("photosCount") + " fotos" + (weight.isEmpty() ? "" : "\nPeso: " + weight + " kg"));
        }
        entries(RecordsApi.WATER, "Consumos de água", "amountMilliliters", " ml", "consumedAt");
        entries(RecordsApi.MEALS, "Refeições realizadas", "mealName", "", "consumedAt");
        entries(RecordsApi.WEIGHT, "Pesagens", "weightKg", " kg", "measuredAt");
    }
    private void entries(String resource, String title, String field, String unit, String timestamp) {
        JSONArray values = vm.items(resource); if (values.length() == 0) return;
        screen.rows.addView(screen.text(title, 20));
        for (int i = 0; i < values.length(); i++) {
            JSONObject record = values.optJSONObject(i); if (record == null) continue;
            LinearLayout card = screen.card(RecordFormatting.value(record, field) + unit, RecordFormatting.time(record.optString(timestamp)) + "\n" + RecordFormatting.value(record, "notes"));
            if (own) {
                MaterialButton edit = screen.button("Editar registro", v -> registrarDialog(RecordDialogs.edit(this, vm, resource, record)));
                MaterialButton remove = screen.button("Remover registro", v -> registrarDialog(RecordDialogs.remove(this, vm, resource, record.optString("id"))));
                edit.setEnabled(vm.ready()); remove.setEnabled(vm.ready()); card.addView(edit); card.addView(remove);
            }
        }
    }
}
