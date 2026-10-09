package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.domain.FollowUpRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryFollowUpRepository implements FollowUpRepository {
    private final ConcurrentMap<UUID, FollowUp> followUps = new ConcurrentHashMap<>();

    @Override
    public Optional<FollowUp> findById(UUID id) {
        return Optional.ofNullable(followUps.get(id));
    }

    @Override
    public List<FollowUp> findByNutritionistId(UUID nutritionistId) {
        return followUps.values().stream()
                .filter(followUp -> followUp.nutritionistId().equals(nutritionistId)).toList();
    }

    @Override
    public List<FollowUp> findByPatientId(UUID patientId) {
        return followUps.values().stream()
                .filter(followUp -> followUp.patientId().equals(patientId)).toList();
    }

    @Override
    public FollowUp save(FollowUp followUp) {
        followUps.put(followUp.id(), followUp);
        return followUp;
    }
}
