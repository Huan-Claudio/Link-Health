package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.domain.FollowUpRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
@Transactional(readOnly = true)
public class PostgresFollowUpRepository implements FollowUpRepository {
    private final FollowUpJpaSpringRepository repository;

    public PostgresFollowUpRepository(FollowUpJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<FollowUp> findById(UUID id) {
        return repository.findById(id).map(FollowUpEntity::toDomain);
    }

    @Override
    public List<FollowUp> findByNutritionistId(UUID nutritionistId) {
        return repository.findAllByNutritionistIdOrderByInvitedAtAsc(nutritionistId).stream()
                .map(FollowUpEntity::toDomain).toList();
    }

    @Override
    public List<FollowUp> findByPatientId(UUID patientId) {
        return repository.findAllByPatientIdOrderByInvitedAtAsc(patientId).stream()
                .map(FollowUpEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public FollowUp save(FollowUp followUp) {
        try {
            return repository.saveAndFlush(FollowUpEntity.from(followUp)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidRequestException("Não foi possível salvar o acompanhamento. Verifique os perfis e o vínculo existente.");
        }
    }
}
