package br.edu.pucgoias.linkhealth.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserAccount(
        UUID id,
        String fullName,
        LocalDate birthDate,
        String email,
        String phone,
        String document,
        String passwordHash,
        AccountRole role,
        Instant createdAt) {
}
