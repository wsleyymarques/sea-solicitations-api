package br.com.sea.solicitations.solicitation.application.service;

import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationCommand;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationResult;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.domain.BasicData;
import br.com.sea.solicitations.solicitation.domain.Solicitation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

public class CreateSolicitationService implements CreateSolicitationUseCase {

    private final SolicitationRepository solicitationRepository;

    public CreateSolicitationService(SolicitationRepository solicitationRepository) {
        this.solicitationRepository = Objects.requireNonNull(
                solicitationRepository,
                "Solicitation repository is required"
        );
    }

    @Override
    @Transactional
    public CreateSolicitationResult create(CreateSolicitationCommand command) {
        Objects.requireNonNull(command, "Create solicitation command is required");

        Solicitation solicitation = new Solicitation(command.clientId());
        BasicData basicData = new BasicData(
            command.serviceType(),
            command.title(),
            command.description()
        );
        solicitation.completeBasicData(basicData);

        UUID id = solicitationRepository.save(solicitation);
        return new CreateSolicitationResult(
                id,
                solicitation.getStatus(),
                solicitation.getCurrentStep()
        );
    }
}
