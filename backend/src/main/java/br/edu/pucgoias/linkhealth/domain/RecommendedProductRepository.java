package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecommendedProductRepository {

    List<RecommendedProduct> findByPatientId(UUID patientId);

    Optional<RecommendedProduct> findByIdAndPatientId(UUID productId, UUID patientId);

    RecommendedProduct save(RecommendedProduct product);

    void delete(RecommendedProduct product);
}
