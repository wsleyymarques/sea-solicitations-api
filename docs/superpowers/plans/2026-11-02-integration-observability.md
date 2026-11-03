# Marco 11 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verificar a API contra PostgreSQL real e concluir contrato HTTP, metricas e inicializacao Compose.

**Architecture:** Testcontainers em perfil Maven separado; OpenAPI central e anotacoes HTTP; metricas na fronteira de processamento; smoke Compose isolado; CI executa os gates.

**Tech Stack:** Java 21, Spring Boot 3.5, Testcontainers, JUnit 5, PostgreSQL, Springdoc, Micrometer, Docker Compose.

**Spec:** `docs/superpowers/specs/2026-11-02-integration-observability-design.md`

## Global Constraints

- Preservar alteracoes locais sem commit automatico.
- Nao requerer Docker no `./mvnw verify` comum.
- Nao expor IDs de usuarios, emails, senhas ou tokens em tags de metricas.

## Review Focus

- Teste HTTP passa por JWT real e banco migrado, sem mock do caminho principal.
- Outbox permanece pendente no teste sem Elasticsearch.
- Contrato OpenAPI marca auth publica e endpoints protegidos corretamente.
- Script Compose nao afeta volumes do projeto principal.
- Falha de auditoria nao impede resposta e incrementa metrica.

---

### Task 1: Integracao real

- [x] Escrever teste HTTP RED para cadastro, login, criacao, autorizacao e persistencia.
- [x] Adicionar Testcontainers e perfil `integration`; executar GREEN.

### Task 2: OpenAPI

- [x] Escrever teste RED para esquema bearer, rotas publicas e erros.
- [x] Implementar configuracao e anotacoes; executar GREEN.

### Task 3: Metricas

- [x] Escrever testes RED para eventos pendentes, resultados e falhas de auditoria.
- [x] Implementar instrumentacao; executar GREEN.

### Task 4: Operacao e entrega

- [x] Criar smoke Compose isolado, executar e atualizar CI.
- [x] Atualizar README e index.
- [x] Executar suite comum, perfil de integracao, smoke e `git diff --check`.
