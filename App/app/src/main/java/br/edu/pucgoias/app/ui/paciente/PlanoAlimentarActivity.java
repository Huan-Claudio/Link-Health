package br.edu.pucgoias.app.ui.paciente;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.lifecycle.ViewModelProvider;

import br.edu.pucgoias.app.BaseActivity;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;
import br.edu.pucgoias.app.model.Perfil;
import br.edu.pucgoias.app.ui.comum.*;
import br.edu.pucgoias.app.util.PatientNavigation;

import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

/** Consulta o plano publicado e persiste as refeições efetivamente realizadas. */
public class PlanoAlimentarActivity extends BaseActivity {
    private RecordsViewModel vm;
    private RecordsScreen screen;
    private boolean own;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (sessaoExpirada()) return;
        UUID patient = PatientNavigation.patientId(getIntent());
        if (patient == null) {
            fecharPacienteNaoEncontrado();
            return;
        }
        own = Sessao.getPerfil() == Perfil.PACIENTE;
        screen = new RecordsScreen(this, getString(R.string.meu_plano_alimentar));
        setContentView(screen.root);
        vm = new ViewModelProvider(this).get(RecordsViewModel.class);
        if (own) {
            MaterialButton add =
                    screen.button(
                            "Registrar outra refeição",
                            v ->
                                    registrarDialog(
                                            RecordDialogs.edit(this, vm, RecordsApi.MEALS, null)));
            add.setId(R.id.recordAddMeal);
            screen.actions.addView(add);
        }
        screen.refresh.setOnClickListener(v -> vm.reload());
        vm.changes.observe(this, ignored -> render());
        vm.configure(patient, Sessao.getAccessToken(), false, RecordsApi.PLAN, RecordsApi.MEALS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null) vm.reload();
    }

    private void render() {
        screen.state(vm);
        screen.rows.removeAllViews();
        JSONArray plan = vm.items(RecordsApi.PLAN), records = vm.items(RecordsApi.MEALS);
        if (plan.length() > 0) screen.rows.addView(screen.text("Seu plano de hoje", 20));
        for (int i = 0; i < plan.length(); i++) {
            JSONObject meal = plan.optJSONObject(i);
            if (meal == null) continue;
            String id = meal.optString("id");
            JSONObject completion = null;
            for (int j = 0; j < records.length(); j++) {
                JSONObject record = records.optJSONObject(j);
                if (record != null
                        && id.equals(record.optString("mealPlanItemId"))
                        && RecordFormatting.today(record.optString("consumedAt"))) {
                    completion = record;
                    break;
                }
            }
            StringBuilder details = new StringBuilder(meal.optString("time", ""));
            JSONArray foods = meal.optJSONArray("foods");
            if (foods != null)
                for (int j = 0; j < foods.length(); j++)
                    details.append("\n").append(foods.optString(j));
            details.append(completion == null ? "\nAinda não realizada hoje" : "\nRealizada hoje");
            LinearLayout card = screen.card(meal.optString("name"), details.toString());
            card.setTag("planned-" + id);
            if (own) {
                JSONObject saved = completion;
                MaterialButton toggle =
                        screen.button(
                                saved == null ? "Marcar como realizada" : "Desmarcar realização",
                                v -> {
                                    if (saved != null)
                                        vm.delete(RecordsApi.MEALS, saved.optString("id"));
                                    else
                                        try {
                                            vm.save(
                                                    RecordsApi.MEALS,
                                                    null,
                                                    new JSONObject()
                                                            .put("mealPlanItemId", id)
                                                            .put(
                                                                    "mealName",
                                                                    meal.optString("name")));
                                        } catch (org.json.JSONException e) {
                                            vm.report("Não foi possível preparar o registro.");
                                        }
                                });
                toggle.setEnabled(vm.ready());
                card.addView(toggle);
            }
        }
        screen.rows.addView(screen.text("Seu diário alimentar", 20));
        if (records.length() == 0 && vm.loading == 0)
            screen.rows.addView(screen.text("Nenhuma refeição registrada.", 15));
        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            if (record == null) continue;
            String note = RecordFormatting.value(record, "notes");
            LinearLayout card =
                    screen.card(
                            record.optString("mealName"),
                            RecordFormatting.time(record.optString("consumedAt"))
                                    + (note.isEmpty() ? "" : "\n" + note));
            if (own) {
                MaterialButton edit =
                        screen.button(
                                "Editar refeição",
                                v ->
                                        registrarDialog(
                                                RecordDialogs.edit(
                                                        this, vm, RecordsApi.MEALS, record)));
                MaterialButton remove =
                        screen.button(
                                "Remover refeição",
                                v ->
                                        registrarDialog(
                                                RecordDialogs.remove(
                                                        this,
                                                        vm,
                                                        RecordsApi.MEALS,
                                                        record.optString("id"))));
                edit.setEnabled(vm.ready());
                remove.setEnabled(vm.ready());
                edit.setTextSize(12);
                remove.setTextSize(12);
                edit.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(getColor(R.color.lh_input)));
                remove.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(getColor(R.color.lh_input)));
                LinearLayout actions = screen.column();
                actions.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -2, 1);
                left.setMargins(0, 0, screen.dp(4), 0);
                actions.addView(edit, left);
                LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -2, 1);
                right.setMargins(screen.dp(4), 0, 0, 0);
                actions.addView(remove, right);
                card.addView(actions);
            }
        }
    }
}
