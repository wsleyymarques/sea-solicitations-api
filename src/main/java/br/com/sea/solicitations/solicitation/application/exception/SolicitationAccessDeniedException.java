package br.com.sea.solicitations.solicitation.application.exception;

public class SolicitationAccessDeniedException extends RuntimeException {

    public SolicitationAccessDeniedException() {
        super("Solicitation does not belong to the authenticated client");
    }
}
