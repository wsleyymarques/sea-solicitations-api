# Solicitation Confirmation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir que o cliente proprietario complete ou edite a confirmacao de uma solicitacao `DRAFT`.

**Architecture:** Um caso de uso carrega e autoriza o agregado, cria `Confirmation`, fornece a data atual por `Clock` e persiste pela porta existente. O controller traduz JWT e DTOs; as regras continuam protegidas pelo dominio.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC, Spring Security, Jakarta Validation, JPA, JUnit 5 e Mockito.

**Spec:** `docs/superpowers/specs/2026-11-01-solicitation-confirmation-design.md`

## Global Constraints

- Somente `CLIENT` proprietario pode alterar a solicitacao.
- O `clientId` vem exclusivamente do `sub` do JWT.
- A solicitacao precisa estar `DRAFT` e ter dados basicos e endereco.
- Hoje e uma data preferida valida; somente datas anteriores sao rejeitadas.
- `HIGH` exige valor estimado de pelo menos `100.00`.
- Nenhuma migration ou integracao externa sera adicionada.

## Review Focus

- Data igual a hoje deve ser aceita, enquanto ontem deve ser rejeitado sem salvar.
- `HIGH` com valor ausente ou `99.99` deve falhar; `100.00` deve ser aceito.
- `LOW` e `MEDIUM` devem permitir `estimatedValue` ausente.
- Edicao de confirmacao em um rascunho ja `COMPLETED` deve permanecer `COMPLETED`.
- Valor que excede `NUMERIC(19,2)` deve falhar na borda antes do caso de uso.

---

### Task 1: Caso de uso da confirmacao

**Files:**
- Create: `src/test/java/br/com/sea/solicitations/solicitation/application/service/CompleteSolicitationConfirmationServiceTest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/CompleteSolicitationConfirmationCommand.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/CompleteSolicitationConfirmationResult.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/CompleteSolicitationConfirmationUseCase.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/service/CompleteSolicitationConfirmationService.java`

**Interfaces:**
- Consumes: `SolicitationRepository.findById(UUID)`, `Solicitation.completeConfirmation(Confirmation, LocalDate)` e `Clock`.
- Produces: `CompleteSolicitationConfirmationUseCase.complete(command) -> result`.

- [x] **Step 1: Escrever os testes falhando do service**

Cobrir sucesso com data igual a hoje, edicao do rascunho completo, solicitacao inexistente, outro proprietario, estado nao editavel, etapas anteriores incompletas, data passada e regra `HIGH`.

- [x] **Step 2: Confirmar RED**

Run: `./mvnw -Dtest=CompleteSolicitationConfirmationServiceTest test`

Expected: FAIL porque contratos e service ainda nao existem.

- [x] **Step 3: Implementar comando, resultado e service**

O service usa `LocalDate.now(clock)`, valida acesso e pre-condicoes antes de alterar o agregado e retorna os dados efetivamente persistidos.

- [x] **Step 4: Confirmar GREEN**

Run: `./mvnw -Dtest=CompleteSolicitationConfirmationServiceTest test`

Expected: PASS.

### Task 2: Endpoint, wiring e documentacao

**Files:**
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/SolicitationControllerTest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/CompleteSolicitationConfirmationRequest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/CompleteSolicitationConfirmationResponse.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/SolicitationController.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/config/SolicitationModuleConfiguration.java`
- Create: `src/main/java/br/com/sea/solicitations/shared/adapter/config/TimeConfiguration.java`
- Modify: `src/main/java/br/com/sea/solicitations/shared/adapter/config/JwtConfiguration.java`
- Modify: `README.md`

**Interfaces:**
- Consumes: `CompleteSolicitationConfirmationUseCase` e `Jwt.sub`.
- Produces: `PUT /solicitations/{id}/confirmation`, `200 OK` e erros JSON existentes.

- [x] **Step 1: Escrever testes HTTP falhando**

Cobrir sucesso e captura do `clientId`, campos ausentes, data passada, termos falsos, valor negativo, excesso de digitos e enum desconhecido.

- [x] **Step 2: Confirmar RED**

Run: `./mvnw -Dtest=SolicitationControllerTest test`

Expected: FAIL porque endpoint e DTOs ainda nao existem.

- [x] **Step 3: Implementar DTOs, endpoint e wiring**

Usar `@FutureOrPresent`, `@PositiveOrZero`, `@Digits(integer = 17, fraction = 2)`, `@AssertTrue` e o `Clock` compartilhado.

- [x] **Step 4: Atualizar README e confirmar regressao**

Run: `./mvnw test`

Expected: BUILD SUCCESS, zero falhas e zero erros.

- [x] **Step 5: Verificar artefatos**

Run: `docker compose --env-file .env.example config --quiet && git diff --check`

Expected: ambos terminam com exit code 0.
