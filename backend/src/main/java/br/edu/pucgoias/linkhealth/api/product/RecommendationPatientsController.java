package br.edu.pucgoias.linkhealth.api.product;

import br.edu.pucgoias.linkhealth.api.error.ForbiddenOperationException;
import br.edu.pucgoias.linkhealth.config.MobilePrincipal;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.FollowUpStatus;
import br.edu.pucgoias.linkhealth.service.FollowUpService;
import br.edu.pucgoias.linkhealth.service.UserAccountService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lista somente os pacientes ativos do nutricionista autenticado. */
@RestController
public class RecommendationPatientsController {
    private final FollowUpService followUps;
    private final UserAccountService accounts;

    public RecommendationPatientsController(FollowUpService followUps, UserAccountService accounts) {
        this.followUps = followUps;
        this.accounts = accounts;
    }

    @GetMapping("/api/v1/nutritionists/me/recommended-product-patients")
    public List<PatientSummary> list(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof MobilePrincipal principal)
                || principal.role() != AccountRole.NUTRICIONISTA) {
            throw new ForbiddenOperationException("Entre como nutricionista para consultar seus pacientes.");
        }
        return followUps.byNutritionist(principal.accountId()).stream()
                .filter(f -> f.status() == FollowUpStatus.ATIVO)
                .map(f -> accounts.find(f.patientId()))
                .map(p -> new PatientSummary(p.id(), p.fullName()))
                .toList();
    }

    public record PatientSummary(UUID id, String name) { }
}
