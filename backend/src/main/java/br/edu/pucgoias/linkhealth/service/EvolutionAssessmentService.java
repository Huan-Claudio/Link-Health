package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.*;
import br.edu.pucgoias.linkhealth.api.evolution.AssessmentRequest;
import br.edu.pucgoias.linkhealth.domain.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class EvolutionAssessmentService {
    private final EvolutionAssessmentRepository repository;
    private final EvolutionPhotoRecordService photos;
    private final WeightRecordService weights;
    private final PatientAccessService access;
    private final UserAccountService accounts;
    private final PatientRecordLock lock;

    public EvolutionAssessmentService(
            EvolutionAssessmentRepository repository,
            EvolutionPhotoRecordService photos,
            WeightRecordService weights,
            PatientAccessService access,
            UserAccountService accounts,
            PatientRecordLock lock) {
        this.repository = repository;
        this.photos = photos;
        this.weights = weights;
        this.access = access;
        this.accounts = accounts;
        this.lock = lock;
    }

    public List<Response> list(UUID patient) {
        access.ensureCanAccess(patient);
        accounts.requirePatient(patient);
        return repository.findByPatientId(patient).stream().map(this::response).toList();
    }

    @Transactional
    public synchronized Response create(
            UUID patient,
            AssessmentRequest request,
            MultipartFile front,
            MultipartFile side,
            MultipartFile back) {
        access.ensureCanWriteRecords(patient);
        accounts.requirePatient(patient);
        lock.lock(patient);
        var files = List.of(front, side, back);
        files.forEach(photos::validateUpload);
        String digest = digest(request, files);
        var previous = repository.findById(request.requestId());
        if (previous.isPresent()) {
            var value = previous.get();
            if (!value.patientId().equals(patient) || !value.requestDigest().equals(digest))
                throw new InvalidRequestException(
                        "A identificação deste envio já foi utilizada por outro registro.");
            return response(value);
        }
        List<UUID> created = new ArrayList<>();
        UUID weight = null;
        Instant at = request.recordedAt() == null ? Instant.now() : request.recordedAt();
        try {
            weight = weights.create(patient, request.weightKg(), at, null).id();
            for (MultipartFile file : files) {
                String type = photos.imageContentType(file);
                var photo =
                        photos.create(
                                patient,
                                "evolucao" + (type.equals("image/png") ? ".png" : ".jpg"),
                                type,
                                at,
                                null);
                created.add(photo.id());
                photos.upload(patient, photo.id(), file);
            }
            var value =
                    repository.save(
                            new EvolutionAssessment(
                                    request.requestId(),
                                    patient,
                                    weight,
                                    created.get(0),
                                    created.get(1),
                                    created.get(2),
                                    at,
                                    digest));
            return response(value);
        } catch (RuntimeException failure) {
            // PostgreSQL reverte a transação; a limpeza também atende o perfil local.
            for (UUID id : created)
                try {
                    photos.delete(patient, id);
                } catch (RuntimeException ignored) {
                }
            if (weight != null)
                try {
                    weights.delete(patient, weight);
                } catch (RuntimeException ignored) {
                }
            throw failure;
        }
    }

    @Transactional
    public synchronized void delete(UUID patient, UUID id) {
        access.ensureCanWriteRecords(patient);
        accounts.requirePatient(patient);
        lock.lock(patient);
        var value =
                repository
                        .findById(id)
                        .filter(v -> v.patientId().equals(patient))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Registro de evolução não encontrado."));
        repository.delete(value);
        for (UUID photo : List.of(value.frontPhotoId(), value.sidePhotoId(), value.backPhotoId()))
            photos.delete(patient, photo);
        weights.delete(patient, value.weightRecordId());
    }

    private Response response(EvolutionAssessment v) {
        BigDecimal weight =
                weights.list(v.patientId()).stream()
                        .filter(w -> w.id().equals(v.weightRecordId()))
                        .findFirst()
                        .orElseThrow()
                        .weightKg();
        return new Response(
                v.id(),
                v.patientId(),
                v.weightRecordId(),
                weight,
                v.recordedAt(),
                List.of(
                        new Photo("FRONT", v.frontPhotoId()),
                        new Photo("SIDE", v.sidePhotoId()),
                        new Photo("BACK", v.backPhotoId())));
    }

    private String digest(AssessmentRequest r, List<MultipartFile> files) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(
                    (r.weightKg().stripTrailingZeros().toPlainString() + "|" + r.recordedAt())
                            .getBytes(StandardCharsets.UTF_8));
            for (var file : files) {
                byte[] bytes = file.getBytes();
                md.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.length).array());
                md.update(bytes);
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new InvalidRequestException("Não foi possível ler as fotos do registro.");
        }
    }

    public record Response(
            UUID id,
            UUID patientId,
            UUID weightRecordId,
            BigDecimal weightKg,
            Instant recordedAt,
            List<Photo> photos) {}

    public record Photo(String position, UUID photoId) {}
}
