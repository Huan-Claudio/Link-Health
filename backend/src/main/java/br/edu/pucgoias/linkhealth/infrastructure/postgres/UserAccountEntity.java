package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_accounts")
public class UserAccountEntity {
    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 13)
    private AccountRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PatientProfileEntity patientProfile;

    @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private NutritionistProfileEntity nutritionistProfile;

    protected UserAccountEntity() {
    }

    static UserAccountEntity from(UserAccount account) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.id = account.id();
        entity.fullName = account.fullName();
        entity.birthDate = account.birthDate();
        entity.email = account.email();
        entity.phone = account.phone();
        entity.passwordHash = account.passwordHash();
        entity.role = account.role();
        entity.createdAt = account.createdAt();
        if (account.role() == AccountRole.PACIENTE) {
            entity.patientProfile = new PatientProfileEntity(entity, account.document());
        } else {
            entity.nutritionistProfile = new NutritionistProfileEntity(entity, account.document());
        }
        return entity;
    }

    UserAccount toDomain() {
        String document = role == AccountRole.PACIENTE ? patientProfile.cpf() : nutritionistProfile.registration();
        return new UserAccount(id, fullName, birthDate, email, phone, document, passwordHash, role, createdAt);
    }
}
