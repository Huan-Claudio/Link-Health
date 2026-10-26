package br.edu.pucgoias.linkhealth.api.evolution;

import br.edu.pucgoias.linkhealth.service.EvolutionAssessmentService;

import jakarta.validation.Valid;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/evolution-assessments")
public class EvolutionAssessmentController {
    private final EvolutionAssessmentService service;

    public EvolutionAssessmentController(EvolutionAssessmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<EvolutionAssessmentService.Response> list(@PathVariable UUID patientId) {
        return service.list(patientId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvolutionAssessmentService.Response> create(
            @PathVariable UUID patientId,
            @Valid @RequestPart("details") AssessmentRequest details,
            @RequestPart("front") MultipartFile front,
            @RequestPart("side") MultipartFile side,
            @RequestPart("back") MultipartFile back) {
        return ResponseEntity.status(201)
                .body(service.create(patientId, details, front, side, back));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID id) {
        service.delete(patientId, id);
        return ResponseEntity.noContent().build();
    }
}
