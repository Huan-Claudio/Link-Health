package br.edu.pucgoias.linkhealth.api.evolution;

import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoContent;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecord;
import br.edu.pucgoias.linkhealth.service.EvolutionPhotoRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/evolution-photos")
@Tag(
        name = "Fotos de evolução",
        description = "Registros e arquivos das fotos de evolução corporal de um paciente")
public class EvolutionPhotoRecordController {

    private final EvolutionPhotoRecordService service;

    public EvolutionPhotoRecordController(EvolutionPhotoRecordService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os registros de fotos de evolução de um paciente")
    public List<EvolutionPhotoRecordResponse> list(@PathVariable UUID patientId) {
        return service.list(patientId).stream().map(EvolutionPhotoRecordResponse::from).toList();
    }

    @PostMapping
    @Operation(summary = "Cria o registro pendente de uma foto de evolução")
    public ResponseEntity<EvolutionPhotoRecordResponse> create(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreateEvolutionPhotoRecordRequest request) {
        EvolutionPhotoRecord record = service.create(
                patientId,
                request.originalFileName(),
                request.contentType(),
                request.capturedAt(),
                request.notes());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{photoId}")
                .buildAndExpand(record.id())
                .toUri();
        return ResponseEntity.created(location).body(EvolutionPhotoRecordResponse.from(record));
    }

    @PostMapping(value = "/{photoId}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia o arquivo JPEG ou PNG de uma foto de evolução")
    public EvolutionPhotoRecordResponse upload(
            @PathVariable UUID patientId,
            @PathVariable UUID photoId,
            @RequestPart("file") MultipartFile file) {
        return EvolutionPhotoRecordResponse.from(service.upload(patientId, photoId, file));
    }

    @GetMapping("/{photoId}/content")
    @Operation(summary = "Obtém o arquivo de uma foto de evolução")
    public ResponseEntity<InputStreamResource> openContent(
            @PathVariable UUID patientId,
            @PathVariable UUID photoId) {
        EvolutionPhotoContent content = service.openContent(patientId, photoId);
        String disposition = ContentDisposition.inline()
                .filename(content.originalFileName(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(new InputStreamResource(content.inputStream()));
    }

    @DeleteMapping("/{photoId}")
    @Operation(summary = "Remove o registro e o arquivo associado de uma foto de evolução")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID photoId) {
        service.delete(patientId, photoId);
        return ResponseEntity.noContent().build();
    }
}
