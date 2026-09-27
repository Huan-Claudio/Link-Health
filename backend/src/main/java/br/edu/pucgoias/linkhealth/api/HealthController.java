package br.edu.pucgoias.linkhealth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Infraestrutura", description = "Verificação básica de disponibilidade da API")
public class HealthController {

    @GetMapping
    @Operation(summary = "Verifica se a API está disponível")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", "link-health-api", Instant.now()));
    }
}
