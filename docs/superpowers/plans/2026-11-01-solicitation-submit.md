# Solicitation Submit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir que o cliente proprietario submeta um rascunho completo e receba o estado persistido da transicao.

**Architecture:** O caso de uso autoriza o cliente e fornece um unico instante do `Clock`; o agregado protege completude, validade e transicao. O controller continua fino e o handler traduz a incompletude para violacoes JSON por etapa.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC, Spring Security, JPA, JUnit 5 e Mockito.

**Spec:** `docs/superpowers/specs/2026-11-01-solicitation-submit-design.md`

## Global Constraints

- Somente o `CLIENT` proprietario pode submeter.
- O `clientId` vem exclusivamente do `sub` do JWT.
- Apenas solicitacoes `DRAFT` e completas podem ser submetidas.
- Data e instante devem derivar do mesmo `Clock`.
- Nenhuma migration ou integracao externa sera adicionada.

## Review Focus

- Uma segunda submissao deve retornar conflito sem salvar novamente.
- Uma data preferida que venceu deve retornar conflito sem alterar o agregado.
- Todas as etapas ausentes devem aparecer uma unica vez e em ordem estavel.
- Ownership deve ser validado antes da completude para nao expor o progresso de outro cliente.
- Falha otimista na persistencia deve continuar sendo traduzida para `409`.

---

### Task 1: Dominio e caso de uso de submissao

**Files:**
- Create: `src/main/java/br/com/sea/solicitations/solicitation/domain/exception/IncompleteSolicitationException.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/domain/Solicitation.java`
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/domain/SolicitationTest.java`
- Create: `src/test/java/br/com/sea/solicitations/solicitation/application/service/SubmitSolicitationServiceTest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/SubmitSolicitationCommand.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/SubmitSolicitationResult.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/SubmitSolicitationUseCase.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/service/SubmitSolicitationService.java`

**Interfaces:**
- Consumes: `SolicitationRepository.findById(UUID)`, `Solicitation.submit(LocalDate, Instant)` e `Clock`.
- Produces: `SubmitSolicitationUseCase.submit(command) -> result` e `IncompleteSolicitationException.missingSteps()`.

- [x] **Step 1: Escrever testes falhando do dominio e service**

Cobrir lista completa e ordenada de etapas ausentes; sucesso; inexistente;
outro proprietario; status nao editavel; incompletude; data preferida vencida;
e ausencia de `save` nas falhas.

- [x] **Step 2: Confirmar RED**

Run: `./mvnw -Dtest=SolicitationTest,SubmitSolicitationServiceTest test`

Expected: FAIL porque exception, contratos e service ainda nao existem.

- [x] **Step 3: Implementar dominio, comando, resultado e service**

O service captura um unico `Instant` e deriva dele a `LocalDate` usando o fuso
do `Clock`. A exception de incompletude carrega uma copia imutavel e nao vazia
das etapas ausentes.

- [x] **Step 4: Confirmar GREEN**

Run: `./mvnw -Dtest=SolicitationTest,SubmitSolicitationServiceTest test`

Expected: PASS.

### Task 2: Endpoint, erros, wiring e documentacao

**Files:**
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/SolicitationControllerTest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/SubmitSolicitationResponse.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/SolicitationController.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/config/SolicitationModuleConfiguration.java`
- Modify: `src/main/java/br/com/sea/solicitations/shared/adapter/in/web/error/GlobalExceptionHandler.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: `SubmitSolicitationUseCase`, `Jwt.sub` e `IncompleteSolicitationException.missingSteps()`.
- Produces: `POST /solicitations/{id}/submit`, `200 OK` e `409 INCOMPLETE_SOLICITATION` com violacoes.

- [x] **Step 1: Escrever testes HTTP falhando**

Cobrir sucesso e captura do `clientId`, UUID invalido, incompletude estruturada,
solicitacao inexistente, outro proprietario, estado invalido, data vencida e
concorrencia.

- [x] **Step 2: Confirmar RED**

Run: `./mvnw -Dtest=SolicitationControllerTest test`

Expected: FAIL porque endpoint, response, wiring e handler ainda nao existem.

- [x] **Step 3: Implementar response, endpoint, handler e wiring**

Mapear `BASIC_DATA`, `ADDRESS` e `CONFIRMATION` respectivamente para
`basicData`, `address` e `confirmation`; qualquer etapa inesperada e erro de
programacao e nao deve ser silenciosamente omitida.

- [x] **Step 4: Atualizar README e confirmar regressao**

Run: `./mvnw test`

Expected: BUILD SUCCESS, zero falhas e zero erros.

- [x] **Step 5: Verificar artefatos**

Run: `docker compose --env-file .env.example config --quiet && git diff --check`

Expected: ambos terminam com exit code 0.
