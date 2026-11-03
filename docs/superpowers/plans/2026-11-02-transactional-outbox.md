# Transactional Outbox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Desacoplar escritas de solicitacoes do Elasticsearch com outbox transacional e retry.

**Architecture:** Um decorador grava solicitacao e evento na mesma transacao; um poller chama um processador que faz claim com lock, indexa o estado atual e registra sucesso ou retry.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Data JPA, PostgreSQL, Flyway, Spring Scheduler, Elasticsearch, JUnit 5 e Mockito.

**Spec:** `docs/superpowers/specs/2026-11-02-transactional-outbox-design.md`

## Global Constraints

- Preservar alteracoes locais existentes; nao criar commit automatico.
- Escrita de negocio nao depende da disponibilidade do Elasticsearch.
- Evento e solicitacao confirmam ou revertem juntos.
- Processamento e pelo menos uma vez e idempotente.

## Review Focus

- Falha na gravacao do evento reverte a solicitacao.
- Duas instancias nao processam o mesmo evento ao mesmo tempo.
- Eventos do mesmo agregado nao invertem a ordem.
- Erro de Elasticsearch agendas retries sem descartar o evento.
- Queda apos indexar e antes do commit permite reprocessar sem duplicar o documento.

---

### Task 1: Escrita transacional

**Files:** substituir decorador sincrono, criar porta e adapter de outbox, configuracao, migration; testar escrita e estado da outbox.

- [x] Escrever testes RED para save + evento sem indexacao sincrona.
- [x] Implementar decorador transacional e persistencia do evento.
- [x] Executar GREEN.

### Task 2: Worker e retry

**Files:** ajustar `OutboxEvent`, criar claim com lock, processador e poller agendado; testar retries e processamento.

- [x] Escrever testes RED para backoff, claim, sucesso, erro e reprocessamento.
- [x] Implementar processador e agendamento.
- [x] Executar GREEN.

### Task 3: Documentacao e regressao

**Files:** README, index e desenhos anteriores afetados.

- [x] Documentar consistencia eventual, retry e operacao.
- [x] Executar teste opcional de persistencia com PostgreSQL real.
- [x] Executar `./mvnw verify` e `git diff --check`.
