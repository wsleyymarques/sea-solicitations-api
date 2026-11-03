package br.com.sea.solicitations.outbox.adapter.config;

import br.com.sea.solicitations.outbox.application.port.out.OutboxStore;
import br.com.sea.solicitations.outbox.application.port.out.OutboxMetrics;
import br.com.sea.solicitations.outbox.application.service.OutboxProcessor;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationRepository;
import br.com.sea.solicitations.solicitation.application.port.out.SolicitationSearchIndex;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class OutboxConfiguration {

    @Bean
    OutboxProcessor outboxProcessor(
            OutboxStore store,
            SolicitationRepository solicitations,
            SolicitationSearchIndex index,
            Clock clock,
            OutboxMetrics metrics
    ) {
        return new OutboxProcessor(store, solicitations, index, clock, metrics);
    }
}
