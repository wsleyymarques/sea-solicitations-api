package br.com.sea.solicitations.solicitation.application.exception;

public class SolicitationNotFoundException extends RuntimeException {

    public SolicitationNotFoundException() {
        super("Solicitation was not found");
    }
}
