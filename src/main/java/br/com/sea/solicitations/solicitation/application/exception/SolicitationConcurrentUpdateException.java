package br.com.sea.solicitations.solicitation.application.exception;

public class SolicitationConcurrentUpdateException extends RuntimeException {

    public SolicitationConcurrentUpdateException() {
        super("Solicitation was updated by another request");
    }

    public SolicitationConcurrentUpdateException(Throwable cause) {
        super("Solicitation was updated by another request", cause);
    }
}
