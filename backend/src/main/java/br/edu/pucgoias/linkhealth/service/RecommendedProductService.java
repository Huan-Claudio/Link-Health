package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.RecommendedProduct;
import br.edu.pucgoias.linkhealth.domain.RecommendedProductRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Currency;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RecommendedProductService {

    private static final String DEFAULT_CURRENCY = "BRL";

    private final RecommendedProductRepository repository;
    private final UserAccountService accounts;

    public RecommendedProductService(RecommendedProductRepository repository, UserAccountService accounts) {
        this.repository = repository;
        this.accounts = accounts;
    }

    public List<RecommendedProduct> list(UUID patientId) {
        accounts.requirePatient(patientId);
        return repository.findByPatientId(patientId);
    }

    public RecommendedProduct create(
            UUID patientId,
            String name,
            String description,
            BigDecimal price,
            String currency,
            String purchaseUrl) {
        accounts.requirePatient(patientId);
        Instant now = Instant.now();
        RecommendedProduct product = new RecommendedProduct(
                UUID.randomUUID(),
                patientId,
                name.trim(),
                trimToNull(description),
                price,
                normalizeCurrency(currency),
                normalizePurchaseUrl(purchaseUrl),
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
                purchaseUrl == null ? current.purchaseUrl() : normalizePurchaseUrl(purchaseUrl),
                current.createdAt(),
                Instant.now());
        return repository.save(updated);
    }

    public void delete(UUID patientId, UUID productId) {
        repository.delete(find(patientId, productId));
    }

    private RecommendedProduct find(UUID patientId, UUID productId) {
        accounts.requirePatient(patientId);
        return repository.findByIdAndPatientId(productId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto recomendado não encontrado."));
    }

    private String normalizeCurrency(String currency) {
        String code = currency == null ? DEFAULT_CURRENCY : currency.trim().toUpperCase(Locale.ROOT);
        try {
            return Currency.getInstance(code).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("Informe uma moeda válida, por exemplo BRL, USD ou EUR.");
        }
    }

    private String normalizePurchaseUrl(String value) {
        String url = trimToNull(value);
        if (url == null) return null;
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getPort() > 65535 || uri.getPort() == 0) {
                throw new IllegalArgumentException();
            }
            return url;
        } catch (IllegalArgumentException exception) {
            throw new InvalidRequestException("O link de compra deve ser um endereço HTTP ou HTTPS válido, sem usuário ou senha.");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
