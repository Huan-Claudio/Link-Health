package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.MeasurementUnit;
import br.edu.pucgoias.linkhealth.domain.ShoppingItem;
import br.edu.pucgoias.linkhealth.domain.ShoppingItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ShoppingItemService {

    private final ShoppingItemRepository repository;

    public ShoppingItemService(ShoppingItemRepository repository) {
        this.repository = repository;
    }

    public List<ShoppingItem> list(UUID patientId) {
        return repository.findByPatientId(patientId);
    }

    public ShoppingItem create(UUID patientId, String name, BigDecimal quantity, MeasurementUnit unit) {
        Instant now = Instant.now();
        ShoppingItem item = new ShoppingItem(
                UUID.randomUUID(), patientId, name.trim(), quantity, unit, false, now, now);
        return repository.save(item);
    }

    public ShoppingItem update(
            UUID patientId,
            UUID itemId,
            String name,
            BigDecimal quantity,
            MeasurementUnit unit) {
        if (name == null && quantity == null && unit == null) {
            throw new InvalidRequestException("Informe ao menos um campo para atualizar o item.");
        }

        ShoppingItem current = find(patientId, itemId);
        ShoppingItem updated = new ShoppingItem(
                current.id(),
                current.patientId(),
                name == null ? current.name() : name.trim(),
                quantity == null ? current.quantity() : quantity,
                unit == null ? current.unit() : unit,
                current.purchased(),
                current.createdAt(),
                Instant.now());
        return repository.save(updated);
    }

    public ShoppingItem updatePurchaseStatus(UUID patientId, UUID itemId, boolean purchased) {
        ShoppingItem current = find(patientId, itemId);
        ShoppingItem updated = new ShoppingItem(
                current.id(),
                current.patientId(),
                current.name(),
                current.quantity(),
                current.unit(),
                purchased,
                current.createdAt(),
                Instant.now());
        return repository.save(updated);
    }

    public void delete(UUID patientId, UUID itemId) {
        repository.delete(find(patientId, itemId));
    }

    private ShoppingItem find(UUID patientId, UUID itemId) {
        return repository.findByIdAndPatientId(itemId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Item da lista de compras não encontrado."));
    }
}
