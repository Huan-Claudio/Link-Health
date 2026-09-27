package br.edu.pucgoias.linkhealth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingItemRepository {

    List<ShoppingItem> findByPatientId(UUID patientId);

    Optional<ShoppingItem> findByIdAndPatientId(UUID itemId, UUID patientId);

    ShoppingItem save(ShoppingItem item);

    void delete(ShoppingItem item);
}
