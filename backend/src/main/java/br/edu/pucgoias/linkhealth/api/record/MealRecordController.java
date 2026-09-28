package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.domain.MealRecord;
import br.edu.pucgoias.linkhealth.service.MealRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/meal-records")
@Tag(name = "Registros de refeições", description = "Refeições realizadas por um paciente")
public class MealRecordController {

    private final MealRecordService service;

    public MealRecordController(MealRecordService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista as refeições registradas para um paciente")
    public List<MealRecordResponse> list(@PathVariable UUID patientId) {
        return service.list(patientId).stream().map(MealRecordResponse::from).toList();
    }

    @PostMapping
    @Operation(summary = "Registra uma refeição realizada")
    public ResponseEntity<MealRecordResponse> create(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreateMealRecordRequest request) {
        MealRecord record = service.create(
                patientId, request.mealPlanItemId(), request.mealName(), request.consumedAt(), request.notes());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{recordId}")
                .buildAndExpand(record.id())
                .toUri();
        return ResponseEntity.created(location).body(MealRecordResponse.from(record));
    }

    @PatchMapping("/{recordId}")
    @Operation(summary = "Edita um registro de refeição")
    public MealRecordResponse update(
            @PathVariable UUID patientId,
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateMealRecordRequest request) {
        return MealRecordResponse.from(service.update(
                patientId, recordId, request.mealName(), request.consumedAt(), request.notes()));
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "Remove um registro de refeição")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID recordId) {
        service.delete(patientId, recordId);
        return ResponseEntity.noContent().build();
    }
}
