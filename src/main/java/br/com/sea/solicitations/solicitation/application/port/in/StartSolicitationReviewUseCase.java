package br.com.sea.solicitations.solicitation.application.port.in;

public interface StartSolicitationReviewUseCase {

    StartSolicitationReviewResult start(StartSolicitationReviewCommand command);
}
