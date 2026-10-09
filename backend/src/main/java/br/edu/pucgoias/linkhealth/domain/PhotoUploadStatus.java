package br.edu.pucgoias.linkhealth.domain;

/**
 * Estado do arquivo da foto de evolução.
 *
 * <p>Todo registro começa pendente. Depois que o arquivo passa pela validação
 * e é guardado no armazenamento configurado, ele passa para armazenado.</p>
 */
public enum PhotoUploadStatus {
    PENDING_UPLOAD,
    STORED
}
