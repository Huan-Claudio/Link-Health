package br.edu.pucgoias.linkhealth.api.account;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import java.time.LocalDate;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String fullName,
        LocalDate birthDate,
        String email,
        String phone,
        String document,
        AccountRole role) {
    public static AccountResponse from(UserAccount account) {
        return new AccountResponse(account.id(), account.fullName(), account.birthDate(), account.email(),
                account.phone(), account.document(), account.role());
    }
}
