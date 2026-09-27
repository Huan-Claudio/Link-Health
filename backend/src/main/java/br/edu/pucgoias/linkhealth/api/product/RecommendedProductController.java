package br.edu.pucgoias.linkhealth.api.product;

import br.edu.pucgoias.linkhealth.domain.RecommendedProduct;
import br.edu.pucgoias.linkhealth.service.RecommendedProductService;
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
@RequestMapping("/api/v1/patients/{patientId}/recommended-products")
@Tag(name = "Produtos recomendados", description = "Produtos indicados para um paciente")
public class RecommendedProductController {

    private final RecommendedProductService service;

    public RecommendedProductController(RecommendedProductService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os produtos recomendados para um paciente")
    public List<RecommendedProductResponse> list(@PathVariable UUID patientId) {
        return service.list(patientId).stream().map(RecommendedProductResponse::from).toList();
    }

    @PostMapping
    @Operation(summary = "Adiciona um produto recomendado")
    public ResponseEntity<RecommendedProductResponse> create(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreateRecommendedProductRequest request) {
        RecommendedProduct product = service.create(
                patientId,
                request.name(),
                request.description(),
                request.price(),
                request.currency(),
                request.purchaseUrl());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}")
                .buildAndExpand(product.id())
                .toUri();
        return ResponseEntity.created(location).body(RecommendedProductResponse.from(product));
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "Edita um produto recomendado")
    public RecommendedProductResponse update(
            @PathVariable UUID patientId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateRecommendedProductRequest request) {
        return RecommendedProductResponse.from(service.update(
                patientId,
                productId,
                request.name(),
                request.description(),
                request.price(),
                request.currency(),
                request.purchaseUrl()));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Remove um produto recomendado")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID productId) {
        service.delete(patientId, productId);
        return ResponseEntity.noContent().build();
    }
}
