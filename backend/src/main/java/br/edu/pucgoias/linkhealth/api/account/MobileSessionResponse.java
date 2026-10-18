package br.edu.pucgoias.linkhealth.api.account;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import java.time.Instant;
import java.util.UUID;

/** Resposta exclusiva do login do aplicativo: conta pública e token temporário. */
public record MobileSessionResponse(
        UUID id,
        String fullName,
        String email,
        AccountRole role,
        String accessToken,
        Instant expiresAt) {

    static MobileSessionResponse from(UserAccount account, String accessToken, Instant expiresAt) {
        return new MobileSessionResponse(
                account.id(), account.fullName(), account.email(), account.role(), accessToken, expiresAt);
    }
}
