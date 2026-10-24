package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.ForbiddenOperationException;
import br.edu.pucgoias.linkhealth.config.MobilePrincipal;
import br.edu.pucgoias.linkhealth.config.LinkHealthSecurityProperties;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.FollowUpRepository;
import br.edu.pucgoias.linkhealth.domain.FollowUpStatus;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Autoriza o paciente titular ou o nutricionista que possui acompanhamento ativo. */
@Service
@Profile({"local", "postgres"})
public class PatientAccessService {

    private final FollowUpRepository followUps;
    private final LinkHealthSecurityProperties security;

    public PatientAccessService(FollowUpRepository followUps, LinkHealthSecurityProperties security) {
        this.followUps = followUps;
        this.security = security;
    }

    public void ensureCanAccess(UUID patientId) {
        if (!security.isEnabled()) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenOperationException("É necessário autenticar-se para acessar os dados do paciente.");
        }

        // A chave do servidor é mantida para Swagger e integrações administrativas, fora do APK.
        if ("link-health-client".equals(authentication.getPrincipal())) {
            return;
        }

        if (authentication.getPrincipal() instanceof MobilePrincipal principal) {
            if (principal.role() == AccountRole.PACIENTE && principal.accountId().equals(patientId)) {
                return;
            }
            if (principal.role() == AccountRole.NUTRICIONISTA && followUps.findByNutritionistId(principal.accountId())
                    .stream()
                    .anyMatch(followUp -> followUp.patientId().equals(patientId)
                            && followUp.status() == FollowUpStatus.ATIVO)) {
                return;
            }
        }
        throw new ForbiddenOperationException("Você não tem permissão para acessar os dados deste paciente.");
    }

    /** Pacientes podem consultar indicações, mas apenas nutricionistas vinculados podem editá-las. */
    public void ensureCanManageRecommendations(UUID patientId) {
        if (!security.isEnabled()) return;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            if ("link-health-client".equals(authentication.getPrincipal())) return;
            if (authentication.getPrincipal() instanceof MobilePrincipal principal
                    && principal.role() == AccountRole.NUTRICIONISTA
                    && followUps.findByNutritionistId(principal.accountId()).stream()
                            .anyMatch(f -> f.patientId().equals(patientId) && f.status() == FollowUpStatus.ATIVO)) return;
        }
        throw new ForbiddenOperationException("Somente o nutricionista vinculado pode alterar as recomendações.");
    }
}
