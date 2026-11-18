package br.edu.pucgoias.linkhealth.api.followup;

import br.edu.pucgoias.linkhealth.domain.*;
import br.edu.pucgoias.linkhealth.service.*;
import java.time.Instant;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class MyFollowUpsController {
    private final ClinicalAccessService access;
    private final FollowUpService service;
    private final UserAccountService accounts;
    public MyFollowUpsController(ClinicalAccessService access, FollowUpService service, UserAccountService accounts) {
        this.access = access; this.service = service; this.accounts = accounts;
    }

    @GetMapping("/api/v1/me/follow-ups")
    public List<Response> list() {
        var p = access.current();
        var links = p.role() == AccountRole.NUTRICIONISTA ? service.byNutritionist(p.accountId()) : service.byPatient(p.accountId());
        return links.stream().sorted(Comparator.comparing(FollowUp::invitedAt).reversed())
                .map(f -> new Response(f.id(), f.nutritionistId(), f.patientId(),
                        accounts.find(f.nutritionistId()).fullName(), accounts.find(f.patientId()).fullName(),
                        f.status(), f.objective(), f.waterGoalMl(), f.invitedAt())).toList();
    }
    public record Response(UUID id, UUID nutritionistId, UUID patientId, String nutritionistName,
            String patientName, FollowUpStatus status, String objective, Integer waterGoalMl, Instant invitedAt) {}
}
