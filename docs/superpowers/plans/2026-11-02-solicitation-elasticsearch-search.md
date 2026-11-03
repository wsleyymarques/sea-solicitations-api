# Solicitation Elasticsearch Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Indexar as transicoes e oferecer busca segura e paginada ao analista.

**Architecture:** Um decorador da porta de repositorio indexa apos cada `save`. Um adapter Spring Data Elasticsearch define mapping e query; o controller apenas traduz JWT e filtros.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring Data Elasticsearch 5.5, Elasticsearch 8.18, JUnit 5 e Mockito.

**Spec:** `docs/superpowers/specs/2026-11-02-solicitation-elasticsearch-search-design.md`

## Global Constraints

- Preservar alteracoes locais existentes; nao criar commit automatico.
- PostgreSQL e fonte de verdade; consistencia garantida por outbox pertence ao Marco 9.
- Cobertura e atribuicao devem entrar obrigatoriamente na query Elasticsearch.

## Review Focus

- Filtro de UF do usuario nao pode ampliar a cobertura.
- Estado `IN_REVIEW` ou terminal de outro analista nao pode aparecer.
- A indexacao de inicio de revisao nao pode ser omitida.
- Cobertura vazia nao pode gerar query sem filtro.
- `submittedTo` inclui o dia completo em UTC, sem overflow em `LocalDate.MAX`.

---

### Task 1: Porta, documento e indexacao

**Files:** criar porta de busca, documento, adapter Elasticsearch e decorador do repositorio; alterar configuracao; testar mapping e indexacao apos `save`.

- [x] Escrever testes de documento e indexacao para cada transicao.
- [x] Executar RED.
- [x] Implementar porta, adapter e decorador que indexa apos `save`.
- [x] Executar GREEN.

### Task 2: Busca segura

**Files:** criar query/resultado/caso de uso de busca e query Elasticsearch; testes de filtros, visibilidade e ordenacao.

- [x] Escrever testes de cobertura, filtros, datas, paginacao e ordenacao.
- [x] Executar RED.
- [x] Implementar consulta segura.
- [x] Executar GREEN.

### Task 3: Endpoint e documentacao

**Files:** alterar controller, configuracao, tratamento de erros e README; testes HTTP.

- [x] Escrever testes de JWT, filtros, erros e RBAC.
- [x] Executar RED.
- [x] Implementar endpoint e documentar.
- [x] Executar suite Maven e `git diff --check`.

`./mvnw verify` passou. O teste com Elasticsearch real nao foi executado
porque o daemon Docker nao esta disponivel neste ambiente.
