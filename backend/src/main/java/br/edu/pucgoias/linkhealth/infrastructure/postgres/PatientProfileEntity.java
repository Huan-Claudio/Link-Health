package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "patients")
public class PatientProfileEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserAccountEntity account;

    @Column(nullable = false, unique = true, length = 30)
    private String cpf;

    protected PatientProfileEntity() {
    }

    PatientProfileEntity(UserAccountEntity account, String cpf) {
        this.account = account;
        this.cpf = cpf;
    }

    String cpf() {
        return cpf;
    }
}
