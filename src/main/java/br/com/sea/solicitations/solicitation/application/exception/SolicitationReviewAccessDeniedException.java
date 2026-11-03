package br.com.sea.solicitations.solicitation.application.exception;

public class SolicitationReviewAccessDeniedException extends RuntimeException {

    public SolicitationReviewAccessDeniedException() {
        super("Analyst is not authorized to review this solicitation");
    }
}
