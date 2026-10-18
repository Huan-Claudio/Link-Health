package br.edu.pucgoias.linkhealth.config;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import java.util.UUID;

/** Identidade mínima carregada pelo token temporário usado pelo aplicativo Android. */
public record MobilePrincipal(UUID accountId, AccountRole role) {
}
