package br.edu.pucgoias.linkhealth.infrastructure.postgres;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.domain.MealPlan;
import br.edu.pucgoias.linkhealth.domain.MealPlanRepository;
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
public class PostgresMealPlanRepository implements MealPlanRepository {
    private final MealPlanJpaSpringRepository repository;

    public PostgresMealPlanRepository(MealPlanJpaSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MealPlan> findById(UUID id) {
        return repository.findById(id).map(MealPlanEntity::toDomain);
    }

    @Override
    public List<MealPlan> findByFollowUpId(UUID followUpId) {
        return repository.findAllByFollowUpId(followUpId).stream().map(MealPlanEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public MealPlan save(MealPlan plan) {
        try {
            return repository.saveAndFlush(MealPlanEntity.from(plan)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidRequestException("Não foi possível salvar o plano. Verifique o acompanhamento, as refeições e os itens.");
        }
    }

    @Override
    @Transactional
    public MealPlan activate(MealPlan plan) {
        repository.findAllByFollowUpIdAndActiveTrue(plan.followUpId()).forEach(MealPlanEntity::deactivate);
        repository.flush();
        return save(plan);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
