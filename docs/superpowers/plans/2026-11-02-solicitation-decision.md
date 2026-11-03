# Solicitation Decision Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir aprovacao ou rejeicao terminal pelo analista atribuido.

**Architecture:** Um caso de uso aplica cobertura, atribuicao e estado ao agregado existente. O controller le ator e cobertura do JWT, e a persistencia usa o `@Version` existente.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC, JPA, JUnit 5, Mockito e AssertJ.

**Spec:** `docs/superpowers/specs/2026-11-02-solicitation-decision-design.md`

## Global Constraints

- Preservar o trabalho local existente e nao criar commit automatico no workspace sujo.
- Nao antecipar a auditoria por AOP do Marco 10.
- Comentario de 10 a 1000 caracteres apos `trim()`.

## Review Focus

- Comentario so com espacos ou com comprimento invalido deve falhar sem salvar.
- Um analista fora da cobertura ou diferente do atribuido nao decide.
- Uma segunda decisao nao altera a primeira.
- Resultado invalido ou corpo ausente deve produzir `400`.
- Atualizacao concorrente deve preservar a primeira decisao.

---

### Task 1: Caso de uso

**Files:** criar `DecideSolicitationCommand`, `DecideSolicitationResult`, `DecideSolicitationUseCase`, `DecisionOutcome` e `DecideSolicitationService`; testar `DecideSolicitationServiceTest` e complementar `SolicitationTest`.

**Interfaces:** `DecideSolicitationUseCase.decide(DecideSolicitationCommand)` retorna `DecideSolicitationResult`; usa `SolicitationRepository.findById/save`.

- [x] Escrever testes para aprovacao, rejeicao, cobertura, atribuicao, estado, comentario e conflito.
- [x] Executar testes e confirmar RED.
- [x] Implementar contratos e service.
- [x] Reexecutar testes e confirmar GREEN.

### Task 2: Contrato HTTP

**Files:** criar `DecideSolicitationRequest` e `DecideSolicitationResponse`; modificar `AnalystSolicitationController` e `SolicitationModuleConfiguration`; testar `AnalystSolicitationControllerTest`.

**Interfaces:** `POST /analyst/solicitations/{id}/decision` recebe `outcome` e `comment`, retorna `200` com decisao registrada.

- [x] Escrever testes para JWT, corpo, validacao e RBAC.
- [x] Executar testes e confirmar RED.
- [x] Implementar endpoint e wiring.
- [x] Reexecutar testes e confirmar GREEN.

### Task 3: Documentacao e regressao

**Files:** modificar `README.md`.

- [x] Documentar chamada, respostas e regras.
- [x] Executar `./mvnw verify`, `git diff --check` e revisar o resultado.

O build completo passou. O teste opt-in com PostgreSQL nao foi executado porque
o daemon Docker nao esta disponivel neste ambiente.
