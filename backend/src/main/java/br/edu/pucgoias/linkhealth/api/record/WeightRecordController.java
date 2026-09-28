package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.domain.WeightRecord;
import br.edu.pucgoias.linkhealth.service.WeightRecordService;
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
@RequestMapping("/api/v1/patients/{patientId}/weight-records")
@Tag(name = "Registros de peso", description = "Histórico de peso de um paciente")
public class WeightRecordController {

    private final WeightRecordService service;

    public WeightRecordController(WeightRecordService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os registros de peso de um paciente")
    public List<WeightRecordResponse> list(@PathVariable UUID patientId) {
        return service.list(patientId).stream().map(WeightRecordResponse::from).toList();
    }

    @PostMapping
    @Operation(summary = "Registra o peso de um paciente")
    public ResponseEntity<WeightRecordResponse> create(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreateWeightRecordRequest request) {
        WeightRecord record = service.create(patientId, request.weightKg(), request.measuredAt(), request.notes());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{recordId}")
                .buildAndExpand(record.id())
                .toUri();
        return ResponseEntity.created(location).body(WeightRecordResponse.from(record));
    }

    @PatchMapping("/{recordId}")
    @Operation(summary = "Edita um registro de peso")
    public WeightRecordResponse update(
            @PathVariable UUID patientId,
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateWeightRecordRequest request) {
        return WeightRecordResponse.from(service.update(
                patientId, recordId, request.weightKg(), request.measuredAt(), request.notes()));
    }

    @DeleteMapping("/{recordId}")
    @Operation(summary = "Remove um registro de peso")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID recordId) {
        service.delete(patientId, recordId);
        return ResponseEntity.noContent().build();
    }
}
