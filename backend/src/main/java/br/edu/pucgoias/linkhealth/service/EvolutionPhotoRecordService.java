package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.error.InvalidRequestException;
import br.edu.pucgoias.linkhealth.api.error.ResourceNotFoundException;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoContent;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecord;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoRecordRepository;
import br.edu.pucgoias.linkhealth.domain.EvolutionPhotoStorage;
import br.edu.pucgoias.linkhealth.domain.PhotoUploadStatus;
import br.edu.pucgoias.linkhealth.domain.PatientRecordLock;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EvolutionPhotoRecordService {

    private static final long MAXIMUM_IMAGE_PIXELS = 36_000_000;

    private final EvolutionPhotoRecordRepository repository;
    private final EvolutionPhotoStorage storage;
    private final long maximumFileSize;
    private final UserAccountService accounts;
    private final PatientAccessService access;
    private final PatientRecordLock lock;

    public EvolutionPhotoRecordService(
            EvolutionPhotoRecordRepository repository,
            EvolutionPhotoStorage storage,
            UserAccountService accounts,
            PatientAccessService access,
            PatientRecordLock lock,
            @Value("${link-health.photos.max-size-bytes}") long maximumFileSize) {
        this.repository = repository;
        this.storage = storage;
        this.maximumFileSize = maximumFileSize;
        this.accounts = accounts;
        this.access = access;
        this.lock = lock;
    }

    public List<EvolutionPhotoRecord> list(UUID patientId) {
        access.ensureCanAccess(patientId);
        accounts.requirePatient(patientId);
        return repository.findByPatientId(patientId);
    }

    public EvolutionPhotoRecord create(
            UUID patientId, String originalFileName, String contentType, Instant capturedAt, String notes) {
        access.ensureCanWriteRecords(patientId);
        accounts.requirePatient(patientId);
        Instant now = Instant.now();
        EvolutionPhotoRecord record = new EvolutionPhotoRecord(
                UUID.randomUUID(),
                patientId,
                originalFileName.trim(),
                contentType.toLowerCase(Locale.ROOT),
                capturedAt == null ? now : capturedAt,
                trimToNull(notes),
                PhotoUploadStatus.PENDING_UPLOAD,
                null,
                now,
                now);
        return repository.save(record);
    }

    @Transactional
    public synchronized EvolutionPhotoRecord upload(UUID patientId, UUID photoId, MultipartFile file) {
        access.ensureCanWriteRecords(patientId);
        accounts.requirePatient(patientId);
        lock.lock(patientId);
        EvolutionPhotoRecord current = find(patientId, photoId);
        if (current.uploadStatus() == PhotoUploadStatus.STORED) {
            throw new InvalidRequestException("A foto deste registro já foi enviada.");
        }

        validateSize(file);
        DetectedImage detectedImage = detectImage(file);
        if (!current.contentType().equals(detectedImage.contentType())) {
            throw new InvalidRequestException(
                    "O arquivo enviado não corresponde ao tipo informado no registro da foto.");
        }

        String storageKey = storeFile(current, file, detectedImage.extension());
        EvolutionPhotoRecord stored = new EvolutionPhotoRecord(
                current.id(),
                current.patientId(),
                current.originalFileName(),
                current.contentType(),
                current.capturedAt(),
                current.notes(),
                PhotoUploadStatus.STORED,
                storageKey,
                current.createdAt(),
                Instant.now());
        try {
            return repository.save(stored);
        } catch (RuntimeException exception) {
            deleteStoredFileAfterFailedSave(storageKey);
            throw exception;
        }
    }

    public EvolutionPhotoContent openContent(UUID patientId, UUID photoId) {
        access.ensureCanAccess(patientId);
        accounts.requirePatient(patientId);
        EvolutionPhotoRecord record = find(patientId, photoId);
        if (record.uploadStatus() != PhotoUploadStatus.STORED || record.storageKey() == null) {
            throw new ResourceNotFoundException("A imagem desta foto de evolução ainda não foi enviada.");
        }

        try {
            return new EvolutionPhotoContent(
                    storage.open(record.storageKey()),
                    record.contentType(),
                    record.originalFileName());
        } catch (NoSuchFileException exception) {
            throw new ResourceNotFoundException("O arquivo desta foto de evolução não foi encontrado.");
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível abrir a foto de evolução.", exception);
        }
    }

    @Transactional
    public synchronized void delete(UUID patientId, UUID photoId) {
        access.ensureCanWriteRecords(patientId);
        accounts.requirePatient(patientId);
        lock.lock(patientId);
        EvolutionPhotoRecord record = find(patientId, photoId);
        if (record.storageKey() != null) {
            try {
                storage.delete(record.storageKey());
            } catch (IOException exception) {
                throw new IllegalStateException("Não foi possível remover o arquivo da foto de evolução.", exception);
            }
        }
        repository.delete(record);
    }

    private EvolutionPhotoRecord find(UUID patientId, UUID photoId) {
        return repository.findByIdAndPatientId(photoId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de foto de evolução não encontrado."));
    }

    private void validateSize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Envie um arquivo de imagem não vazio.");
        }
        if (file.getSize() > maximumFileSize) {
            throw new InvalidRequestException("A imagem deve ter no máximo 5 MB.");
        }
    }

    private DetectedImage detectImage(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(8);
            DetectedImage detected = detectBySignature(header);
            if (detected == null) {
                throw new InvalidRequestException("Envie somente uma imagem JPEG ou PNG válida.");
            }
            validateReadableImage(file);
            return detected;
        } catch (IOException exception) {
            throw new InvalidRequestException("Não foi possível ler a imagem enviada.");
        }
    }

    private DetectedImage detectBySignature(byte[] header) {
        boolean jpeg = header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
        if (jpeg) {
            return new DetectedImage("image/jpeg", "jpg");
        }

        boolean png = header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
        return png ? new DetectedImage("image/png", "png") : null;
    }

    private void validateReadableImage(MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) {
                throw new InvalidRequestException("Envie uma imagem JPEG ou PNG válida.");
            }
            var readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new InvalidRequestException("Envie uma imagem JPEG ou PNG válida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                // Verifica o tamanho antes de alocar o bitmap completo na memória.
                if (width <= 0 || height <= 0 || (long) width * height > MAXIMUM_IMAGE_PIXELS) {
                    throw new InvalidRequestException("A imagem tem dimensões muito grandes.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) throw new InvalidRequestException("Envie uma imagem JPEG ou PNG válida.");
            } finally {
                reader.dispose();
            }
        }
    }

    private String storeFile(EvolutionPhotoRecord record, MultipartFile file, String extension) {
        try (InputStream input = file.getInputStream()) {
            return storage.store(record.patientId(), record.id(), extension, input);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível armazenar a foto de evolução.", exception);
        }
    }

    private void deleteStoredFileAfterFailedSave(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (IOException ignored) {
            // A exceção do banco é a causa principal; a limpeza será tentada posteriormente.
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DetectedImage(String contentType, String extension) {
    }
}
