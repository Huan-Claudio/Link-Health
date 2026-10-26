package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import java.time.DateTimeException;
import java.time.ZoneId;

public final class RecordDates {
    public static final String DEFAULT_ZONE = "America/Sao_Paulo";
    private RecordDates() { }

    public static ZoneId zone(String zoneId) {
        try {
            return ZoneId.of(zoneId == null ? DEFAULT_ZONE : zoneId);
        } catch (DateTimeException exception) {
            throw new InvalidRequestException("O fuso horário informado é inválido.");
        }
    }
}
