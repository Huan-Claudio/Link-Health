package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.ForbiddenOperationException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.config.LinkHealthSecurityProperties;
import br.edu.pucgoias.linkhealth.config.MobilePrincipal;
import br.edu.pucgoias.linkhealth.domain.*;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Autorização das contas, vínculos e prescrições; conserva o acesso administrativo. */
@Service
public class ClinicalAccessService {
    private final LinkHealthSecurityProperties security;
    private final FollowUpRepository followUps;
    private final MealPlanRepository plans;

    public ClinicalAccessService(LinkHealthSecurityProperties security, FollowUpRepository followUps,
            MealPlanRepository plans) {
        this.security = security;
        this.followUps = followUps;
        this.plans = plans;
    }

    private boolean administrative() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return !security.isEnabled() || (auth != null && auth.isAuthenticated()
                && "link-health-client".equals(auth.getPrincipal()));
    }

    public MobilePrincipal current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof MobilePrincipal p) return p;
        throw denied();
    }

    public void nutritionist() {
        if (!administrative() && current().role() != AccountRole.NUTRICIONISTA) throw denied();
    }

    public void owner(UUID id, AccountRole role) {
        if (administrative()) return;
        var p = current();
        if (!p.accountId().equals(id) || p.role() != role) throw denied();
    }

    public void followUp(UUID id, boolean writePlan) {
        if (administrative()) return;
        FollowUp f = followUps.findById(id).orElseThrow(() -> new ResourceNotFoundException("Acompanhamento não encontrado."));
        var p = current();
        boolean nutritionist = p.role() == AccountRole.NUTRICIONISTA && p.accountId().equals(f.nutritionistId());
        boolean patient = p.role() == AccountRole.PACIENTE && p.accountId().equals(f.patientId());
        if (writePlan ? !nutritionist || f.status() != FollowUpStatus.ATIVO
                : !(nutritionist || patient) || f.status() != FollowUpStatus.ATIVO) throw denied();
    }

    public void plan(UUID id, boolean write) {
        if (administrative()) return;
        MealPlan plan = plans.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado."));
        followUp(plan.followUpId(), write);
        if (!write && current().role() == AccountRole.PACIENTE && !plan.active()) throw denied();
    }

    public boolean patientSession() { return !administrative() && current().role() == AccountRole.PACIENTE; }

    private ForbiddenOperationException denied() { return new ForbiddenOperationException("Você não tem permissão para realizar esta ação."); }
}
