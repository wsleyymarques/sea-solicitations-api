package br.com.sea.solicitations.outbox.application.port.out;

public interface OutboxMetrics {
    void processed();
    void retry();
}
