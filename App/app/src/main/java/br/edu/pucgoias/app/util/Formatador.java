package br.edu.pucgoias.app.util;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Funções de formatação usadas em várias telas. */
public final class Formatador {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Formatador() {
    }

    /** 2.0 -> "2L" | 4.5 -> "4,5L" */
    public static String litros(double valor) {
        DecimalFormat df = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(PT_BR));
        return df.format(valor) + "L";
    }

    /** 70.0 -> "70" | 70.5 -> "70,5" */
    public static String numero(double valor) {
        DecimalFormat df = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(PT_BR));
        return df.format(valor);
    }

    /** Converte "70,5" ou "70.5" em número. Retorna -1 se for inválido. */
    public static double lerNumero(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static String hoje() {
        return new SimpleDateFormat("dd/MM/yyyy", PT_BR).format(new Date());
    }

    /** Porcentagem (0–100) para as barras de progresso. */
    public static int porcentagem(double valor, double total) {
        if (total <= 0) return 0;
        return (int) Math.min(100, Math.round(valor * 100 / total));
    }

    /** Abre o calendário e escreve a data escolhida (dd/mm/aaaa) no campo. */
    public static void configurarCampoData(Context context, EditText campo) {
        campo.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.YEAR, -20);
            new DatePickerDialog(context, (view, ano, mes, dia) ->
                    campo.setText(String.format(PT_BR, "%02d/%02d/%04d", dia, mes + 1, ano)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                    .show();
        });
    }
}
