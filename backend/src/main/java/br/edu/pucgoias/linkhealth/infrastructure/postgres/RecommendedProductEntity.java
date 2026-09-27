package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.RecommendedProduct;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recommended_products")
public class RecommendedProductEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "product_name", nullable = false, length = 160)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "purchase_url", length = 2048)
    private String purchaseUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RecommendedProductEntity() {
    }

    private RecommendedProductEntity(RecommendedProduct product) {
        this.id = product.id();
        this.patientId = product.patientId();
        this.name = product.name();
        this.description = product.description();
        this.price = product.price();
        this.currency = product.currency();
        this.purchaseUrl = product.purchaseUrl();
        this.createdAt = product.createdAt();
        this.updatedAt = product.updatedAt();
    }

    static RecommendedProductEntity from(RecommendedProduct product) {
        return new RecommendedProductEntity(product);
    }

    RecommendedProduct toDomain() {
        return new RecommendedProduct(id, patientId, name, description, price, currency, purchaseUrl, createdAt, updatedAt);
    }
}
