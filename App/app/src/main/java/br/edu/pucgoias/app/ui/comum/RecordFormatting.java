package br.edu.pucgoias.app.ui.comum;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public final class RecordFormatting {
    // Mesmo dia clínico usado pela unicidade das refeições no PostgreSQL.
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    public static LocalDate todayDate() { return LocalDate.now(ZONE); }
    private RecordFormatting() { }
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    public static String time(String iso) {
        try { return Instant.parse(iso).atZone(ZONE).format(DATE_TIME); }
        catch (RuntimeException e) { return iso; }
    }
    public static String date(String iso) {
        try { return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }
        catch (RuntimeException e) { return iso; }
    }
    public static boolean today(String iso) {
        try { return Instant.parse(iso).atZone(ZONE).toLocalDate().equals(todayDate()); }
        catch (RuntimeException e) { return false; }
    }
    public static String value(JSONObject json, String key) { return json.isNull(key) ? "" : json.optString(key, ""); }

    /**
     * Nome do registro que guarda a "anotação do dia" do paciente (botão "Adicionar anotação" no
     * Meu Plano Alimentar). A API não tem um recurso próprio para anotações, então ela é gravada
     * como um registro de refeição com este nome; as telas não a tratam como refeição.
     */
    public static final String ANOTACAO_DO_DIA = "Anotação do dia";

    public static boolean isAnotacao(JSONObject registro) {
        return ANOTACAO_DO_DIA.equals(registro.optString("mealName"));
    }
}
