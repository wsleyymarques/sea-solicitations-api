package br.com.sea.solicitations.solicitation.adapter.config;

import br.com.sea.solicitations.solicitation.application.port.in.CreateSolicitationUseCase;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.service.CreateSolicitationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SolicitationModuleConfiguration {

    @Bean
    CreateSolicitationUseCase createSolicitationUseCase(
            SolicitationRepository solicitationRepository
    ) {
        return new CreateSolicitationService(solicitationRepository);
    }
}
