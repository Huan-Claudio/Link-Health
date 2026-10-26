package br.edu.pucgoias.app.ui.comum;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import br.edu.pucgoias.app.R;
import br.edu.pucgoias.app.data.remote.RecordsApi;

public final class RecordDialogs {
    private RecordDialogs() { }
    public static AlertDialog edit(Context context, RecordsViewModel vm, String resource, JSONObject existing) {
        boolean water = RecordsApi.WATER.equals(resource), weight = RecordsApi.WEIGHT.equals(resource);
        String field = water ? "amountMilliliters" : weight ? "weightKg" : "mealName";
        String dateField = weight ? "measuredAt" : "consumedAt";
        String label = water ? "Quantidade em ml" : weight ? "Peso em kg" : "Nome da refeição";
        LinearLayout layout = new LinearLayout(context); layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * context.getResources().getDisplayMetrics().density); layout.setPadding(pad, 0, pad, 0);
        EditText value = field(context, layout, label, R.id.recordInputValue);
        value.setInputType(water ? InputType.TYPE_CLASS_NUMBER : weight ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT);
        EditText date = field(context, layout, "Data e hora: dd/MM/aaaa HH:mm", R.id.recordInputDate);
        EditText notes = field(context, layout, "Observação (opcional)", R.id.recordInputNotes);
        if (water) notes.setVisibility(android.view.View.GONE);
        value.setText(existing == null ? "" : RecordFormatting.value(existing, field));
        date.setText(RecordFormatting.time(existing == null ? Instant.now().toString() : existing.optString(dateField)));
        notes.setText(existing == null ? "" : RecordFormatting.value(existing, "notes"));
        AlertDialog dialog = new MaterialAlertDialogBuilder(context).setTitle(existing == null ? "Novo registro" : "Editar registro")
                .setView(layout).setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            try {
                String text = value.getText().toString().trim();
                if (text.isEmpty()) { value.setError("Preencha este campo."); return; }
                JSONObject body = new JSONObject();
                if (water) {
                    int amount = Integer.parseInt(text);
                    if (amount < 1 || amount > 10000) { value.setError("Informe de 1 a 10000 ml."); return; }
                    body.put(field, amount);
                } else if (weight) {
                    BigDecimal amount = new BigDecimal(text.replace(',', '.'));
                    if (amount.compareTo(BigDecimal.ONE) < 0 || amount.compareTo(new BigDecimal("500")) > 0 || amount.scale() > 2) {
                        value.setError("Informe de 1 a 500 kg, com até duas casas decimais."); return;
                    }
                    body.put(field, amount);
                } else {
                    if (text.length() > 120) { value.setError("Use até 120 caracteres."); return; }
                    body.put(field, text);
                }
                Instant timestamp;
                try { timestamp = LocalDateTime.parse(date.getText().toString().trim(), RecordFormatting.DATE_TIME).atZone(RecordFormatting.ZONE).toInstant(); }
                catch (RuntimeException invalid) { date.setError("Use dd/MM/aaaa HH:mm."); return; }
                if (timestamp.isAfter(Instant.now())) { date.setError("A data não pode estar no futuro."); return; }
                body.put(dateField, timestamp.toString());
                if (!water) {
                    String note = notes.getText().toString().trim();
                    if (note.length() > 500) { notes.setError("Use até 500 caracteres."); return; }
                    body.put("notes", note);
                }
                vm.save(resource, existing == null ? null : existing.optString("id"), body); dialog.dismiss();
            } catch (Exception invalid) { value.setError("Confira o valor informado."); }
        }));
        dialog.show(); return dialog;
    }
    private static EditText field(Context context, LinearLayout parent, String hint, int id) {
        EditText input = new EditText(context); input.setId(id); input.setHint(hint); input.setSingleLine(false);
        parent.addView(input, new LinearLayout.LayoutParams(-1, -2)); return input;
    }
    public static AlertDialog remove(Context context, RecordsViewModel vm, String resource, String id) {
        return new MaterialAlertDialogBuilder(context).setTitle("Remover registro?")
                .setMessage("O registro será removido do seu histórico.").setNegativeButton("Cancelar", null)
                .setPositiveButton("Remover", (d, w) -> vm.delete(resource, id)).show();
    }
}
