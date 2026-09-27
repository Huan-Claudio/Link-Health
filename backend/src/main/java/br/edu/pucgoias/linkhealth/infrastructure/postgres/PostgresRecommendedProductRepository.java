package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.RecommendedProduct;
import br.edu.pucgoias.linkhealth.domain.RecommendedProductRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class PostgresRecommendedProductRepository implements RecommendedProductRepository {

    private final RecommendedProductJpaSpringRepository repository;

    public PostgresRecommendedProductRepository(RecommendedProductJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<RecommendedProduct> findByPatientId(UUID patientId) {
        return repository.findAllByPatientIdOrderByCreatedAtAsc(patientId).stream()
                .map(RecommendedProductEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<RecommendedProduct> findByIdAndPatientId(UUID productId, UUID patientId) {
        return repository.findByIdAndPatientId(productId, patientId).map(RecommendedProductEntity::toDomain);
    }

    @Override
    public RecommendedProduct save(RecommendedProduct product) {
        return repository.save(RecommendedProductEntity.from(product)).toDomain();
    }

    @Override
    public void delete(RecommendedProduct product) {
        repository.deleteById(product.id());
    }
}
