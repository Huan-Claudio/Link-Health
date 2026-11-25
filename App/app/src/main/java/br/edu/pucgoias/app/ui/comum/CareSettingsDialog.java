package br.edu.pucgoias.app.ui.comum;

import android.app.Activity;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.Sessao;
import br.edu.pucgoias.app.data.remote.RecordsApi;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Diálogo da Ficha do paciente que edita UM campo por vez: o objetivo (card do objetivo)
 * ou a meta diária de água (botão "Editar meta").
 * A API grava os dois campos juntos, então o campo que não aparece é reenviado com o valor
 * carregado do servidor, sem alteração.
 */
public final class CareSettingsDialog {

    /** Qual informação o diálogo permite alterar. */
    public enum Campo { OBJETIVO, META_AGUA }

    private CareSettingsDialog() {}

    public static AlertDialog show(Activity activity, UUID patient, Campo campo, Runnable onSaved) {
        boolean editaObjetivo = campo == Campo.OBJETIVO;
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * activity.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, 0, pad, 0);
        TextView status = new TextView(activity);
        status.setText("Carregando acompanhamento…");
        layout.addView(status);

        EditText input = new EditText(activity);
        if (editaObjetivo) {
            input.setId(R.id.careObjectiveInput);
            input.setHint("Objetivo do paciente");
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        } else {
            input.setId(R.id.careGoalInput);
            input.setHint("Meta diária de água em litros (ex.: 3)");
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }
        input.setVisibility(View.INVISIBLE);
        layout.addView(input);

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(editaObjetivo ? "Editar objetivo" : "Editar meta de água")
                        .setView(layout)
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Salvar", null)
                        .create();
        RecordsApi api = new RecordsApi();
        String token = Sessao.getAccessToken();
        boolean[] loaded = {false};
        // Valores atuais no servidor: o campo que não é editado volta sem mudança.
        Object[] objetivoAtual = {JSONObject.NULL};
        Object[] metaAtual = {JSONObject.NULL};
        Runnable[] load = {null};
        load[0] =
                () -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    api.care(
                            patient,
                            token,
                            new RecordsApi.Callback<JSONObject>() {
                                public void onSuccess(JSONObject value) {
                                    activity.runOnUiThread(
                                            () -> {
                                                if (!dialog.isShowing()) return;
                                                loaded[0] = true;
                                                String objetivo =
                                                        RecordFormatting.value(value, "objective");
                                                objetivoAtual[0] =
                                                        objetivo.isEmpty()
                                                                ? JSONObject.NULL
                                                                : objetivo;
                                                metaAtual[0] =
                                                        value.isNull("waterGoalMl")
                                                                ? JSONObject.NULL
                                                                : value.optInt("waterGoalMl");
                                                if (editaObjetivo) input.setText(objetivo);
                                                else
                                                    input.setText(
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
                                                input.setVisibility(View.VISIBLE);
                                                status.setText(
                                                        "Essas informações aparecerão na home do"
                                                            + " paciente.");
                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                        .setText("Salvar");
                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                        .setEnabled(true);
                                            });
                                }

                                public void onError(String message) {
                                    activity.runOnUiThread(
                                            () -> {
                                                if (!dialog.isShowing()) return;
                                                status.setText(message);
                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                        .setText("Tentar novamente");
                                                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                        .setEnabled(true);
                                            });
                                }
                            });
                };
        dialog.setOnShowListener(
                d -> {
                    load[0].run();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setOnClickListener(
                                    v -> {
                                        if (!loaded[0]) {
                                            load[0].run();
                                            return;
                                        }
                                        Object objetivo = objetivoAtual[0];
                                        Object meta = metaAtual[0];
                                        String texto = input.getText().toString().trim();
                                        if (editaObjetivo) {
                                            if (texto.length() > 100) {
                                                input.setError("Use até 100 caracteres.");
                                                return;
                                            }
                                            objetivo = texto.isEmpty() ? JSONObject.NULL : texto;
                                        } else {
                                            try {
                                                Integer ml =
                                                        texto.isEmpty()
                                                                ? null
                                                                : new BigDecimal(
                                                                                texto.replace(
                                                                                        ',', '.'))
                                                                        .multiply(
                                                                                new BigDecimal(
                                                                                        "1000"))
                                                                        .intValueExact();
                                                if (ml != null && (ml < 1 || ml > 10000)) {
                                                    input.setError(
                                                            "Informe uma meta maior que zero e de"
                                                                + " até 10 L.");
                                                    return;
                                                }
                                                meta = ml == null ? JSONObject.NULL : ml;
                                            } catch (RuntimeException e) {
                                                input.setError("Confira a quantidade em litros.");
                                                return;
                                            }
                                        }
                                        JSONObject body;
                                        try {
                                            body =
                                                    new JSONObject()
                                                            .put("objective", objetivo)
                                                            .put("waterGoalMl", meta);
                                        } catch (org.json.JSONException e) {
                                            status.setText("Não foi possível preparar os dados.");
                                            return;
                                        }
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                .setEnabled(false);
                                        api.saveCare(
                                                patient,
                                                token,
                                                body,
                                                new RecordsApi.Callback<JSONObject>() {
                                                    public void onSuccess(JSONObject value) {
                                                        activity.runOnUiThread(
                                                                () -> {
                                                                    if (!dialog.isShowing()) return;
                                                                    dialog.dismiss();
                                                                    onSaved.run();
                                                                });
                                                    }

                                                    public void onError(String message) {
                                                        activity.runOnUiThread(
                                                                () -> {
                                                                    if (!dialog.isShowing()) return;
                                                                    status.setText(message);
                                                                    dialog.getButton(
                                                                                    AlertDialog
                                                                                            .BUTTON_POSITIVE)
                                                                            .setEnabled(true);
                                                                });
                                                    }
                                                });
                                    });
                });
        dialog.show();
        return dialog;
    }
}
