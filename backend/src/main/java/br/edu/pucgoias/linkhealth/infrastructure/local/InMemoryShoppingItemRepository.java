package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.ShoppingItem;
import br.edu.pucgoias.linkhealth.domain.ShoppingItemRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryShoppingItemRepository implements ShoppingItemRepository {

    private final ConcurrentMap<UUID, ShoppingItem> items = new ConcurrentHashMap<>();

    @Override
    public List<ShoppingItem> findByPatientId(UUID patientId) {
        return items.values().stream()
                .filter(item -> item.patientId().equals(patientId))
                .sorted(Comparator.comparing(ShoppingItem::createdAt))
                .toList();
    }

    @Override
    public Optional<ShoppingItem> findByIdAndPatientId(UUID itemId, UUID patientId) {
        return Optional.ofNullable(items.get(itemId))
                .filter(item -> item.patientId().equals(patientId));
    }

    @Override
    public ShoppingItem save(ShoppingItem item) {
        items.put(item.id(), item);
        return item;
    }

    @Override
    public void delete(ShoppingItem item) {
        items.remove(item.id());
    }
}
