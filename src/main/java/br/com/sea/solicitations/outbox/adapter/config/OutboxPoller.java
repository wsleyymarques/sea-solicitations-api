package br.com.sea.solicitations.outbox.adapter.config;

import br.com.sea.solicitations.outbox.application.service.OutboxProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ConditionalOnProperty(prefix = "outbox.polling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPoller {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPoller.class);

    private final OutboxProcessor processor;

    public OutboxPoller(OutboxProcessor processor) {
        this.processor = Objects.requireNonNull(processor, "Outbox processor is required");
    }

    @Scheduled(
            fixedDelayString = "${outbox.poll-interval-ms:5000}",
            initialDelayString = "${outbox.initial-delay-ms:5000}"
    )
    public void poll() {
        try {
            processor.processBatch();
        } catch (RuntimeException exception) {
            LOGGER.error("Outbox processing failed; the next poll will retry", exception);
        }
    }
}
