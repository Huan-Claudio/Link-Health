package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.RecommendedProduct;
import br.edu.pucgoias.linkhealth.domain.RecommendedProductRepository;
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
public class InMemoryRecommendedProductRepository implements RecommendedProductRepository {

    private final ConcurrentMap<UUID, RecommendedProduct> products = new ConcurrentHashMap<>();

    @Override
    public List<RecommendedProduct> findByPatientId(UUID patientId) {
        return products.values().stream()
                .filter(product -> product.patientId().equals(patientId))
                .sorted(Comparator.comparing(RecommendedProduct::createdAt))
                .toList();
    }

    @Override
    public Optional<RecommendedProduct> findByIdAndPatientId(UUID productId, UUID patientId) {
        return Optional.ofNullable(products.get(productId))
                .filter(product -> product.patientId().equals(patientId));
    }

    @Override
    public RecommendedProduct save(RecommendedProduct product) {
        products.put(product.id(), product);
        return product;
    }

    @Override
    public void delete(RecommendedProduct product) {
        products.remove(product.id());
    }
}
