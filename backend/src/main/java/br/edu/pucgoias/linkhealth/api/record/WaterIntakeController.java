package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.domain.WaterIntake;
import br.edu.pucgoias.linkhealth.service.WaterIntakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/water-intakes")
@Tag(name = "Registros de água", description = "Consumo de água registrado por um paciente")
public class WaterIntakeController {

    private final WaterIntakeService service;

    public WaterIntakeController(WaterIntakeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os registros de água de um paciente")
    public List<WaterIntakeResponse> list(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.list(patientId, date).stream().map(WaterIntakeResponse::from).toList();
    }

    @PostMapping
    @Operation(summary = "Registra o consumo de água")
    public ResponseEntity<WaterIntakeResponse> create(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreateWaterIntakeRequest request) {
        WaterIntake intake = service.create(patientId, request.amountMilliliters(), request.consumedAt());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{intakeId}")
                .buildAndExpand(intake.id())
                .toUri();
        return ResponseEntity.created(location).body(WaterIntakeResponse.from(intake));
    }

    @PatchMapping("/{intakeId}")
    @Operation(summary = "Edita um registro de água")
    public WaterIntakeResponse update(
            @PathVariable UUID patientId,
            @PathVariable UUID intakeId,
            @Valid @RequestBody UpdateWaterIntakeRequest request) {
        return WaterIntakeResponse.from(service.update(
                patientId, intakeId, request.amountMilliliters(), request.consumedAt()));
    }

    @DeleteMapping("/{intakeId}")
    @Operation(summary = "Remove um registro de água")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID intakeId) {
        service.delete(patientId, intakeId);
        return ResponseEntity.noContent().build();
    }
}
