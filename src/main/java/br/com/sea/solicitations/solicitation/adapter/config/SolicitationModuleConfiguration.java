package br.com.sea.solicitations.solicitation.adapter.config;

import br.com.sea.solicitations.outbox.application.port.out.OutboxStore;
import br.com.sea.solicitations.solicitation.adapter.out.persistence.JpaSolicitationRepositoryAdapter;
import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.DecideSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationAddressUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.CompleteSolicitationConfirmationUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.ListAnalystSolicitationsUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.SearchSolicitationsUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.SubmitSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.in.StartSolicitationReviewUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.PostalCodeLookup;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationSearchIndex;
import br.com.sea.solicitations.solicitation.application.service.CompleteSolicitationAddressService;
import br.com.sea.solicitations.solicitation.application.service.CompleteSolicitationConfirmationService;
import br.com.sea.solicitations.solicitation.application.service.CreateSolicitationService;
import br.com.sea.solicitations.solicitation.application.service.DecideSolicitationService;
import br.com.sea.solicitations.solicitation.application.service.ListAnalystSolicitationsService;
import br.com.sea.solicitations.solicitation.application.service.OutboxSolicitationRepository;
import br.com.sea.solicitations.solicitation.application.service.SearchSolicitationsService;
import br.com.sea.solicitations.solicitation.application.service.SubmitSolicitationService;
import br.com.sea.solicitations.solicitation.application.service.StartSolicitationReviewService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class SolicitationModuleConfiguration {

    @Bean
    @Primary
    SolicitationRepository outboxSolicitationRepository(
            JpaSolicitationRepositoryAdapter database,
            OutboxStore outbox,
            Clock clock
    ) {
        return new OutboxSolicitationRepository(database, outbox, clock);
    }

    @Bean
    CreateSolicitationUseCase createSolicitationUseCase(
            SolicitationRepository solicitationRepository
    ) {
        return new CreateSolicitationService(solicitationRepository);
    }

    @Bean
    CompleteSolicitationAddressUseCase completeSolicitationAddressUseCase(
            SolicitationRepository solicitationRepository,
            PostalCodeLookup postalCodeLookup
    ) {
        return new CompleteSolicitationAddressService(
                solicitationRepository,
                postalCodeLookup
        );
    }

    @Bean
    CompleteSolicitationConfirmationUseCase completeSolicitationConfirmationUseCase(
            SolicitationRepository solicitationRepository,
            Clock clock
    ) {
        return new CompleteSolicitationConfirmationService(
                solicitationRepository,
                clock
        );
    }

    @Bean
    SubmitSolicitationUseCase submitSolicitationUseCase(
            SolicitationRepository solicitationRepository,
            Clock clock
    ) {
        return new SubmitSolicitationService(solicitationRepository, clock);
    }

    @Bean
    StartSolicitationReviewUseCase startSolicitationReviewUseCase(
            SolicitationRepository solicitationRepository,
            Clock clock
    ) {
        return new StartSolicitationReviewService(solicitationRepository, clock);
    }

    @Bean
    DecideSolicitationUseCase decideSolicitationUseCase(
            SolicitationRepository solicitationRepository,
            Clock clock
    ) {
        return new DecideSolicitationService(solicitationRepository, clock);
    }

    @Bean
    ListAnalystSolicitationsUseCase listAnalystSolicitationsUseCase(
            SolicitationRepository solicitationRepository
    ) {
        return new ListAnalystSolicitationsService(solicitationRepository);
    }

    @Bean
    SearchSolicitationsUseCase searchSolicitationsUseCase(
            SolicitationSearchIndex index,
            SolicitationRepository solicitationRepository
    ) {
        return new SearchSolicitationsService(index, solicitationRepository);
    }
}
