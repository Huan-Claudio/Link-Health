package br.edu.pucgoias.linkhealth.api.shopping;

import br.edu.pucgoias.linkhealth.domain.ShoppingItem;
import br.edu.pucgoias.linkhealth.service.ShoppingItemService;
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
@RequestMapping("/api/v1/patients/{patientId}/shopping-items")
@Tag(name = "Lista de compras", description = "Itens da lista de compras de um paciente")
public class ShoppingItemController {

    private final ShoppingItemService service;

    public ShoppingItemController(ShoppingItemService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista os itens de compra de um paciente")
    public List<ShoppingItemResponse> list(@PathVariable UUID patientId) {
        return service.list(patientId).stream().map(ShoppingItemResponse::from).toList();
    }

    @PostMapping
    @Operation(summary = "Adiciona um item à lista de compras")
    public ResponseEntity<ShoppingItemResponse> create(
            @PathVariable UUID patientId,
            @Valid @RequestBody CreateShoppingItemRequest request) {
        ShoppingItem item = service.create(patientId, request.name(), request.quantity(), request.unit());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{itemId}")
                .buildAndExpand(item.id())
                .toUri();
        return ResponseEntity.created(location).body(ShoppingItemResponse.from(item));
    }

    @PatchMapping("/{itemId}")
    @Operation(summary = "Edita um item da lista de compras")
    public ShoppingItemResponse update(
            @PathVariable UUID patientId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateShoppingItemRequest request) {
        return ShoppingItemResponse.from(service.update(
                patientId, itemId, request.name(), request.quantity(), request.unit()));
    }

    @PatchMapping("/{itemId}/purchase-status")
    @Operation(summary = "Marca ou desmarca um item como comprado")
    public ShoppingItemResponse updatePurchaseStatus(
            @PathVariable UUID patientId,
            @PathVariable UUID itemId,
            @RequestBody PurchaseStatusRequest request) {
        return ShoppingItemResponse.from(service.updatePurchaseStatus(patientId, itemId, request.purchased()));
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Remove um item da lista de compras")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID itemId) {
        service.delete(patientId, itemId);
        return ResponseEntity.noContent().build();
    }
}
