package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.domain.FollowUpStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "follow_ups")
public class FollowUpEntity {
    @Id
    private UUID id;

    @Column(name = "nutritionist_id", nullable = false)
    private UUID nutritionistId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FollowUpStatus status;

    @Column(length = 100)
    private String objective;

    @Column(name = "water_goal_ml")
    private Integer waterGoalMl;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    protected FollowUpEntity() {
    }

    static FollowUpEntity from(FollowUp followUp) {
        FollowUpEntity entity = new FollowUpEntity();
        entity.id = followUp.id();
        entity.nutritionistId = followUp.nutritionistId();
        entity.patientId = followUp.patientId();
        entity.status = followUp.status();
        entity.objective = followUp.objective();
        entity.waterGoalMl = followUp.waterGoalMl();
        entity.invitedAt = followUp.invitedAt();
        return entity;
    }

    FollowUp toDomain() {
        return new FollowUp(id, nutritionistId, patientId, status, objective, waterGoalMl, invitedAt);
    }
}
