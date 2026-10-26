package br.edu.pucgoias.linkhealth.infrastructure.local;

import br.edu.pucgoias.linkhealth.domain.*;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class LocalEvolutionAssessmentRepository implements EvolutionAssessmentRepository {
    private final Map<UUID, EvolutionAssessment> items = new ConcurrentHashMap<>();

    public Optional<EvolutionAssessment> findById(UUID id) {
        return Optional.ofNullable(items.get(id));
    }

    public List<EvolutionAssessment> findByPatientId(UUID id) {
        return items.values().stream()
                .filter(v -> v.patientId().equals(id))
                .sorted(Comparator.comparing(EvolutionAssessment::recordedAt).reversed())
                .toList();
    }

    public EvolutionAssessment save(EvolutionAssessment value) {
        items.put(value.id(), value);
        return value;
    }

    public void delete(EvolutionAssessment value) {
        items.remove(value.id());
    }

    public boolean referencesPhoto(UUID id) {
        return items.values().stream()
                .anyMatch(
                        v ->
                                v.frontPhotoId().equals(id)
                                        || v.sidePhotoId().equals(id)
                                        || v.backPhotoId().equals(id));
    }

    public boolean referencesWeight(UUID id) {
        return items.values().stream().anyMatch(v -> v.weightRecordId().equals(id));
    }
}
