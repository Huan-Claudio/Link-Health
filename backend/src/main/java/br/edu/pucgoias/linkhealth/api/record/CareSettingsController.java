package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.api.error.*;
import br.edu.pucgoias.linkhealth.config.MobilePrincipal;
import br.edu.pucgoias.linkhealth.domain.*;
import br.edu.pucgoias.linkhealth.service.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/care-settings")
public class CareSettingsController {
    private final FollowUpService service;

    public CareSettingsController(FollowUpService service) {
        this.service = service;
    }

    private FollowUp current(UUID patient, Authentication auth) {
        if (auth == null
                || !(auth.getPrincipal() instanceof MobilePrincipal p)
                || p.role() != AccountRole.NUTRICIONISTA)
            throw new ForbiddenOperationException(
                    "Entre como nutricionista vinculado para definir objetivo e meta.");
        return service.byNutritionist(p.accountId()).stream()
                .filter(f -> f.patientId().equals(patient) && f.status() == FollowUpStatus.ATIVO)
                .max(Comparator.comparing(FollowUp::invitedAt))
                .orElseThrow(
                        () ->
                                new ForbiddenOperationException(
                                        "Não existe acompanhamento ativo com este paciente."));
    }

    @GetMapping
    public Response get(@PathVariable UUID patientId, Authentication auth) {
        return Response.from(current(patientId, auth));
    }

    @PatchMapping
    public Response save(
            @PathVariable UUID patientId,
            Authentication auth,
            @Valid @RequestBody Request request) {
        var f = current(patientId, auth);
        return Response.from(
                service.updateDetails(
                        f.id(), f.nutritionistId(), request.objective(), request.waterGoalMl()));
    }

    public record Request(
            @Size(max = 100) String objective, @Min(1) @Max(10000) Integer waterGoalMl) {}

    public record Response(String objective, Integer waterGoalMl) {
        static Response from(FollowUp f) {
            return new Response(f.objective(), f.waterGoalMl());
        }
    }
}
