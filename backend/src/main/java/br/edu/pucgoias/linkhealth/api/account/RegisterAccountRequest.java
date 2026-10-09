package br.edu.pucgoias.linkhealth.api.account;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterAccountRequest(
        @NotBlank @Size(max = 150) String fullName,
        LocalDate birthDate,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @NotBlank @Size(max = 30) String document,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotNull AccountRole role) {
}
