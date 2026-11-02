package br.com.sea.solicitations.solicitation.application.exception;

public class InvalidSolicitationStateException extends RuntimeException {

    public InvalidSolicitationStateException(String message) {
        super(message);
    }
}
