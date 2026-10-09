package br.edu.pucgoias.linkhealth.api.evolution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateEvolutionPhotoRecordRequest(
        @NotBlank(message = "O nome original do arquivo é obrigatório.")
        @Size(max = 255, message = "O nome do arquivo deve ter no máximo 255 caracteres.")
        String originalFileName,
        @NotBlank(message = "O tipo da imagem é obrigatório.")
        @Pattern(
                regexp = "^image/(jpeg|png)$",
                message = "A imagem deve ser JPEG ou PNG.")
        String contentType,
        @PastOrPresent(message = "A data da foto não pode estar no futuro.")
        Instant capturedAt,
        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres.")
        String notes) {
}
