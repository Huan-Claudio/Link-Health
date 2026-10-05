package br.edu.pucgoias.app.util;

import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import br.edu.pucgoias.app.R;

/** Funções de formatação usadas em várias telas. */
public final class Formatador {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String TAG_SELETOR_DATA = "seletor_data_nascimento";

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

    /**
     * Ao tocar no campo, abre o MaterialDatePicker (calendário do Material Design 3) e escreve
     * a data escolhida (dd/mm/aaaa). Só permite datas até hoje (data de nascimento).
     */
    public static void configurarCampoData(FragmentActivity activity, EditText campo) {
        FragmentManager fm = activity.getSupportFragmentManager();

        // Ao girar a tela o FragmentManager recria o seletor aberto, mas sem o listener: reconecta aqui.
        Fragment aberto = fm.findFragmentByTag(TAG_SELETOR_DATA);
        if (aberto instanceof MaterialDatePicker) {
            @SuppressWarnings("unchecked")
            MaterialDatePicker<Long> seletor = (MaterialDatePicker<Long>) aberto;
            conectar(seletor, campo);
        }

        campo.setOnClickListener(v -> {
            // Evita abrir dois calendários com um toque duplo.
            if (fm.findFragmentByTag(TAG_SELETOR_DATA) != null) return;

            CalendarConstraints restricoes = new CalendarConstraints.Builder()
                    .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                    .setValidator(DateValidatorPointBackward.now())
                    .build();
            MaterialDatePicker<Long> seletor = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.label_data_nascimento)
                    .setSelection(selecaoInicial(campo.getText().toString()))
                    .setCalendarConstraints(restricoes)
                    .build();
            conectar(seletor, campo);
            seletor.show(fm, TAG_SELETOR_DATA);
        });
    }

    private static void conectar(MaterialDatePicker<Long> seletor, EditText campo) {
        seletor.addOnPositiveButtonClickListener(selecao -> campo.setText(formatarDataUtc(selecao)));
    }

    /** O MaterialDatePicker trabalha em milissegundos UTC; usa a data já digitada ou "hoje - 20 anos". */
    private static long selecaoInicial(String textoAtual) {
        SimpleDateFormat formato = formatoUtc();
        try {
            Date data = formato.parse(textoAtual.trim());
            if (data != null) return data.getTime();
        } catch (ParseException ignorada) {
            // campo vazio ou fora do padrão: usa a data padrão
        }
        Calendar padrao = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        padrao.setTimeInMillis(MaterialDatePicker.todayInUtcMilliseconds());
        padrao.add(Calendar.YEAR, -20);
        return padrao.getTimeInMillis();
    }

    private static String formatarDataUtc(long milissegundosUtc) {
        return formatoUtc().format(new Date(milissegundosUtc));
    }

    private static SimpleDateFormat formatoUtc() {
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", PT_BR);
        formato.setTimeZone(TimeZone.getTimeZone("UTC"));
        formato.setLenient(false);
        return formato;
    }
}
