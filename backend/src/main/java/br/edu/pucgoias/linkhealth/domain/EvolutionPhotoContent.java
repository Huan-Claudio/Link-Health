package br.edu.pucgoias.linkhealth.domain;

import java.io.InputStream;

/**
 * Arquivo de uma foto pronto para ser devolvido pela API.
 */
public record EvolutionPhotoContent(
        InputStream inputStream,
        String contentType,
        String originalFileName) {
}
