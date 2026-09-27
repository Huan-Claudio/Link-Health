package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.RecommendedProduct;
import br.edu.pucgoias.linkhealth.domain.RecommendedProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecommendedProductService {

    private static final String DEFAULT_CURRENCY = "BRL";

    private final RecommendedProductRepository repository;

    public RecommendedProductService(RecommendedProductRepository repository) {
        this.repository = repository;
    }

    public List<RecommendedProduct> list(UUID patientId) {
        return repository.findByPatientId(patientId);
    }

    public RecommendedProduct create(
            UUID patientId,
            String name,
            String description,
            BigDecimal price,
            String currency,
            String purchaseUrl) {
        Instant now = Instant.now();
        RecommendedProduct product = new RecommendedProduct(
                UUID.randomUUID(),
                patientId,
                name.trim(),
                trimToNull(description),
                price,
                normalizeCurrency(currency),
                trimToNull(purchaseUrl),
                now,
                now);
        return repository.save(product);
    }

    public RecommendedProduct update(
            UUID patientId,
            UUID productId,
            String name,
            String description,
            BigDecimal price,
            String currency,
            String purchaseUrl) {
        if (name == null && description == null && price == null && currency == null && purchaseUrl == null) {
            throw new InvalidRequestException("Informe ao menos um campo para atualizar o produto.");
        }

        RecommendedProduct current = find(patientId, productId);
        RecommendedProduct updated = new RecommendedProduct(
                current.id(),
                current.patientId(),
                name == null ? current.name() : name.trim(),
                description == null ? current.description() : trimToNull(description),
                price == null ? current.price() : price,
                currency == null ? current.currency() : normalizeCurrency(currency),
                purchaseUrl == null ? current.purchaseUrl() : trimToNull(purchaseUrl),
                current.createdAt(),
                Instant.now());
        return repository.save(updated);
    }

    public void delete(UUID patientId, UUID productId) {
        repository.delete(find(patientId, productId));
    }

    private RecommendedProduct find(UUID patientId, UUID productId) {
        return repository.findByIdAndPatientId(productId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto recomendado não encontrado."));
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? DEFAULT_CURRENCY : currency.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
