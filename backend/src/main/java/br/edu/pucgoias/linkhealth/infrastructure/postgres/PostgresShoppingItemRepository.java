package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.ShoppingItem;
import br.edu.pucgoias.linkhealth.domain.ShoppingItemRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresShoppingItemRepository implements ShoppingItemRepository {

    private final ShoppingItemJpaSpringRepository repository;

    public PostgresShoppingItemRepository(ShoppingItemJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ShoppingItem> findByPatientId(UUID patientId) {
        return repository.findAllByPatientIdOrderByCreatedAtAsc(patientId).stream()
                .map(ShoppingItemEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<ShoppingItem> findByIdAndPatientId(UUID itemId, UUID patientId) {
        return repository.findByIdAndPatientId(itemId, patientId).map(ShoppingItemEntity::toDomain);
    }

    @Override
    public ShoppingItem save(ShoppingItem item) {
        return repository.save(ShoppingItemEntity.from(item)).toDomain();
    }

    @Override
    public void delete(ShoppingItem item) {
        repository.deleteById(item.id());
    }
}
