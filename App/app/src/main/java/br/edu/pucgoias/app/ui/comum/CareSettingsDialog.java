package br.edu.pucgoias.app.ui.comum;

import android.app.Activity;
import android.text.InputType;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.UUID;

public final class CareSettingsDialog {
    private CareSettingsDialog() {}

    public static AlertDialog show(Activity activity, UUID patient, Runnable onSaved) {
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, 0, pad, 0);
        TextView status = new TextView(activity);
        status.setText("Carregando acompanhamento…");
        layout.addView(status);
        EditText objective = new EditText(activity);
        objective.setHint("Objetivo do paciente");
        objective.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(objective);
        EditText goal = new EditText(activity);
        goal.setHint("Meta diária de água em litros (ex.: 3)");
        goal.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(goal);
        objective.setId(br.edu.pucgoias.app.R.id.careObjectiveInput);
        goal.setId(br.edu.pucgoias.app.R.id.careGoalInput);
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("Objetivo e meta de água")
                        .setView(layout)
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Salvar", null)
                        .create();
        RecordsApi api = new RecordsApi();
        String token = Sessao.getAccessToken();
        boolean[] loaded = {false};
        Runnable[] load = {null};
        load[0] =
                () -> {
                    dialog.getButton(-1).setEnabled(false);
                    api.care(
                            patient,
                            token,
                            new RecordsApi.Callback<JSONObject>() {
                                public void onSuccess(JSONObject value) {
                                    activity.runOnUiThread(
                                            () -> {
                                                if (!dialog.isShowing()) return;
                                                loaded[0] = true;
                                                objective.setText(
                                                        RecordFormatting.value(value, "objective"));
                                                goal.setText(
                                                        value.isNull("waterGoalMl")
                                                                ? ""
                                                                : new BigDecimal(
                                                                                value.optString(
                                                                                        "waterGoalMl"))
                                                                        .divide(
                                                                                new BigDecimal(
                                                                                        "1000"))
                                                                        .stripTrailingZeros()
                                                                        .toPlainString());
                                                status.setText(
                                                        "Essas informações aparecerão na home do"
                                                            + " paciente.");
                                                dialog.getButton(-1).setText("Salvar");
                                                dialog.getButton(-1).setEnabled(true);
                                            });
                                }

                                public void onError(String message) {
                                    activity.runOnUiThread(
                                            () -> {
                                                if (!dialog.isShowing()) return;
                                                status.setText(message);
                                                dialog.getButton(-1).setText("Tentar novamente");
                                                dialog.getButton(-1).setEnabled(true);
                                            });
                                }
                            });
                };
        dialog.setOnShowListener(
                d -> {
                    load[0].run();
                    dialog.getButton(-1)
                            .setOnClickListener(
                                    v -> {
                                        if (!loaded[0]) {
                                            load[0].run();
                                            return;
                                        }
                                        try {
                                            String text = objective.getText().toString().trim();
                                            if (text.length() > 100) {
                                                objective.setError("Use até 100 caracteres.");
                                                return;
                                            }
                                            String raw = goal.getText().toString().trim();
                                            Integer ml =
                                                    raw.isEmpty()
                                                            ? null
                                                            : new BigDecimal(raw.replace(',', '.'))
                                                                    .multiply(
                                                                            new BigDecimal("1000"))
                                                                    .intValueExact();
                                            if (ml != null && (ml < 1 || ml > 10000)) {
                                                goal.setError(
                                                        "Informe uma meta maior que zero e de até"
                                                            + " 10 L.");
                                                return;
                                            }
                                            JSONObject body =
                                                    new JSONObject()
                                                            .put(
                                                                    "objective",
                                                                    text.isEmpty()
                                                                            ? JSONObject.NULL
                                                                            : text)
                                                            .put(
                                                                    "waterGoalMl",
                                                                    ml == null
                                                                            ? JSONObject.NULL
                                                                            : ml);
                                            dialog.getButton(-1).setEnabled(false);
                                            api.saveCare(
                                                    patient,
                                                    token,
                                                    body,
                                                    new RecordsApi.Callback<JSONObject>() {
                                                        public void onSuccess(JSONObject value) {
                                                            activity.runOnUiThread(
                                                                    () -> {
                                                                        if (!dialog.isShowing())
                                                                            return;
                                                                        dialog.dismiss();
                                                                        onSaved.run();
                                                                    });
                                                        }

                                                        public void onError(String message) {
                                                            activity.runOnUiThread(
                                                                    () -> {
                                                                        if (!dialog.isShowing())
                                                                            return;
                                                                        status.setText(message);
                                                                        dialog.getButton(-1)
                                                                                .setEnabled(true);
                                                                    });
                                                        }
                                                    });
                                        } catch (Exception e) {
                                            goal.setError("Confira a quantidade em litros.");
                                        }
                                    });
                });
        dialog.show();
        return dialog;
    }
}
