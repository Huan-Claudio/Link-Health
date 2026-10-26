package br.edu.pucgoias.app.ui.comum;

import android.content.Context;
import android.text.InputType;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;

import br.edu.pucgoias.app.R;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.math.BigDecimal;

public final class WaterDialog {
    private WaterDialog() {}

    public static AlertDialog show(
            Context context, RecordsViewModel vm, int current, boolean correction) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * context.getResources().getDisplayMetrics().density);
        layout.setPadding(pad, 0, pad, 0);
        TextView help = new TextView(context);
        help.setText(
                correction
                        ? "Informe o total correto que você já bebeu hoje. O valor substituirá o"
                              + " total registrado."
                        : "Informe a quantidade que você acabou de beber.");
        layout.addView(help);
        EditText amount = new EditText(context);
        amount.setId(R.id.waterCustomAmount);
        amount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setHint("Quantidade");
        if (correction) amount.setText(Integer.toString(current));
        layout.addView(amount);
        Spinner unit = new Spinner(context);
        unit.setAdapter(
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_spinner_dropdown_item,
                        new String[] {"ml", "L"}));
        layout.addView(unit);
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(context)
                        .setTitle(correction ? "Corrigir total de hoje" : "Adicionar consumo")
                        .setView(layout)
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton(correction ? "Corrigir total" : "Registrar", null)
                        .create();
        dialog.setOnShowListener(
                d ->
                        dialog.getButton(-1)
                                .setOnClickListener(
                                        v -> {
                                            try {
                                                BigDecimal value =
                                                        new BigDecimal(
                                                                amount.getText()
                                                                        .toString()
                                                                        .trim()
                                                                        .replace(',', '.'));
                                                if (unit.getSelectedItemPosition() == 1)
                                                    value = value.multiply(new BigDecimal("1000"));
                                                int ml = value.intValueExact();
                                                if (ml < (correction ? 0 : 1)
                                                        || ml > (correction ? 50000 : 10000))
                                                    throw new ArithmeticException();
                                                if (correction) vm.correctWater(ml);
                                                else vm.water(ml);
                                                dialog.dismiss();
                                            } catch (RuntimeException e) {
                                                amount.setError(
                                                        correction
                                                                ? "Informe de 0 a 50000 ml, sem"
                                                                      + " fração de ml."
                                                                : "Informe de 1 a 10000 ml, sem"
                                                                      + " fração de ml.");
                                            }
                                        }));
        dialog.show();
        return dialog;
    }
}
