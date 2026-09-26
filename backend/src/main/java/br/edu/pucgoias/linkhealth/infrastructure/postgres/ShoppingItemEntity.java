package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.MeasurementUnit;
import br.edu.pucgoias.linkhealth.domain.ShoppingItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shopping_items")
public class ShoppingItemEntity {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "item_name", nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MeasurementUnit unit;

    @Column(nullable = false)
    private boolean purchased;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShoppingItemEntity() {
    }

    private ShoppingItemEntity(ShoppingItem item) {
        this.id = item.id();
        this.patientId = item.patientId();
        this.name = item.name();
        this.quantity = item.quantity();
        this.unit = item.unit();
        this.purchased = item.purchased();
        this.createdAt = item.createdAt();
        this.updatedAt = item.updatedAt();
    }

    static ShoppingItemEntity from(ShoppingItem item) {
        return new ShoppingItemEntity(item);
    }

    ShoppingItem toDomain() {
        return new ShoppingItem(id, patientId, name, quantity, unit, purchased, createdAt, updatedAt);
    }
}
