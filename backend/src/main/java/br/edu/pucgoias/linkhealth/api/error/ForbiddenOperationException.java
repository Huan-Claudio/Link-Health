package br.edu.pucgoias.linkhealth.api.error;

/** Indica que o usuário autenticado não pode acessar o recurso solicitado. */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
