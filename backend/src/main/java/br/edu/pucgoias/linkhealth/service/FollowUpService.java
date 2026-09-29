package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.AccountRole;
import br.edu.pucgoias.linkhealth.domain.FollowUp;
import br.edu.pucgoias.linkhealth.domain.FollowUpRepository;
import br.edu.pucgoias.linkhealth.domain.FollowUpStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class FollowUpService {
    private final FollowUpRepository repository;
    private final UserAccountService accounts;

    public FollowUpService(FollowUpRepository repository, UserAccountService accounts) {
        this.repository = repository;
        this.accounts = accounts;
    }

    public synchronized FollowUp invite(UUID nutritionistId, UUID patientId, String objective, Integer waterGoalMl) {
        if (accounts.find(nutritionistId).role() != AccountRole.NUTRICIONISTA) {
            throw new InvalidRequestException("O convite deve ser feito por um nutricionista.");
        }
        if (accounts.find(patientId).role() != AccountRole.PACIENTE) {
            throw new InvalidRequestException("O convite deve ser enviado a um paciente.");
        }
        boolean existing = repository.findByNutritionistId(nutritionistId).stream()
                .anyMatch(item -> item.patientId().equals(patientId)
                        && item.status() != FollowUpStatus.RECUSADO);
        if (existing) {
            throw new InvalidRequestException("Já existe um convite ou acompanhamento para este paciente.");
        }
        if (waterGoalMl != null && waterGoalMl <= 0) {
            throw new InvalidRequestException("A meta de água deve ser maior que zero.");
        }
        return repository.save(new FollowUp(UUID.randomUUID(), nutritionistId, patientId,
                FollowUpStatus.PENDENTE, objective == null ? null : objective.trim(), waterGoalMl, Instant.now()));
    }

    public synchronized FollowUp respond(UUID followUpId, UUID patientId, boolean accept) {
        FollowUp current = find(followUpId);
        if (!current.patientId().equals(patientId)) {
            throw new InvalidRequestException("O convite não pertence a este paciente.");
        }
        if (current.status() != FollowUpStatus.PENDENTE) {
            throw new InvalidRequestException("Este convite já foi respondido.");
        }
        return repository.save(new FollowUp(current.id(), current.nutritionistId(), current.patientId(),
                accept ? FollowUpStatus.ATIVO : FollowUpStatus.RECUSADO,
                current.objective(), current.waterGoalMl(), current.invitedAt()));
    }

    public FollowUp find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Acompanhamento não encontrado."));
    }

    public List<FollowUp> byNutritionist(UUID nutritionistId) {
        if (accounts.find(nutritionistId).role() != AccountRole.NUTRICIONISTA) {
            throw new InvalidRequestException("Usuário não é nutricionista.");
        }
        return repository.findByNutritionistId(nutritionistId);
    }

    public List<FollowUp> byPatient(UUID patientId) {
        if (accounts.find(patientId).role() != AccountRole.PACIENTE) {
            throw new InvalidRequestException("Usuário não é paciente.");
        }
        return repository.findByPatientId(patientId);
    }
}
