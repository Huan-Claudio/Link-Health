package br.edu.pucgoias.linkhealth.api.followup;

import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.service.FollowUpService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
@RequestMapping("/api/v1")
public class FollowUpController {
    private final FollowUpService service;

    public FollowUpController(FollowUpService service) {
        this.service = service;
    }

    @PostMapping("/follow-ups")
    public ResponseEntity<FollowUp> invite(@Valid @RequestBody InviteRequest request) {
        FollowUp followUp = service.invite(request.nutritionistId(), request.patientId(),
                request.objective(), request.waterGoalMl());
        return ResponseEntity.status(HttpStatus.CREATED).body(followUp);
    }

    @PostMapping("/follow-ups/{id}/response")
    public FollowUp respond(@PathVariable UUID id, @Valid @RequestBody RespondRequest request) {
        return service.respond(id, request.patientId(), request.accept());
    }

    @GetMapping("/nutritionists/{id}/follow-ups")
    public List<FollowUp> byNutritionist(@PathVariable UUID id) {
        return service.byNutritionist(id);
    }

    @GetMapping("/patients/{id}/follow-ups")
    public List<FollowUp> byPatient(@PathVariable UUID id) {
        return service.byPatient(id);
    }
}
