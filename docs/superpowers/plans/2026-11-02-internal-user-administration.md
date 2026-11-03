# Internal User Administration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expor `POST /admin/users` para um administrador criar analistas com cobertura ou outros administradores sem cobertura.

**Architecture:** O controller administrativo aplica Bean Validation e delega a um caso de uso independente de Spring MVC. O service reutiliza o agregado `User`, `UserRepository` e `PasswordHasher`; a persistencia JPA existente grava a cobertura em `analyst_coverages`.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC/Security, Spring Data JPA, PostgreSQL, JUnit 5, Mockito e AssertJ.

**Spec:** `docs/superpowers/specs/2026-11-02-internal-user-administration-design.md`

## Global Constraints

- Preservar as alteracoes locais existentes e nao criar commits automaticos.
- A rota publica `/auth/register` continua criando somente `CLIENT`.
- Senha bruta nao aparece em JSON de resposta, logs, mensagens ou `toString`.
- `ANALYST` exige cobertura nao vazia; `ADMIN` exige cobertura vazia; `CLIENT` e rejeitado.
- Reutilizar portas e adapters existentes, sem nova dependencia.

## Review Focus

- Papel `CLIENT` nunca pode ser criado pela rota administrativa.
- Senha bruta nao pode vazar em serializacao ou `toString`.
- E-mail duplicado deve falhar antes de hashing e persistencia.
- Analista sem cobertura e administrador com cobertura devem retornar `400`.
- Usuario sem papel `ADMIN` nao pode chamar o caso de uso.

---

### Task 1: Caso de uso e invariantes de papel/cobertura

**Files:**
- Create: `src/main/java/br/com/sea/solicitations/user/application/port/in/CreateInternalUserCommand.java`
- Create: `src/main/java/br/com/sea/solicitations/user/application/port/in/CreateInternalUserResult.java`
- Create: `src/main/java/br/com/sea/solicitations/user/application/port/in/CreateInternalUserUseCase.java`
- Create: `src/main/java/br/com/sea/solicitations/user/application/service/CreateInternalUserService.java`
- Create: `src/test/java/br/com/sea/solicitations/user/application/service/CreateInternalUserServiceTest.java`

**Interfaces:**
- Consumes: `UserRepository`, `PasswordHasher`, `User`, `UserRole` e `FederativeUnit`.
- Produces: `CreateInternalUserUseCase.create(CreateInternalUserCommand)` e resultado sem senha.

- [x] Escrever testes falhando para analista, administrador, e-mail duplicado e combinacoes invalidas de papel/cobertura.
- [x] Executar `./mvnw -Dtest=CreateInternalUserServiceTest test` e confirmar RED por tipos ausentes.
- [x] Implementar command, result, use case e service minimos.
- [x] Reexecutar e confirmar GREEN.

### Task 2: Endpoint administrativo e RBAC

**Files:**
- Create: `src/main/java/br/com/sea/solicitations/user/adapter/in/web/AdminUserController.java`
- Create: `src/main/java/br/com/sea/solicitations/user/adapter/in/web/dto/CreateInternalUserRequest.java`
- Create: `src/main/java/br/com/sea/solicitations/user/adapter/in/web/dto/CreateInternalUserResponse.java`
- Modify: `src/main/java/br/com/sea/solicitations/user/adapter/config/UserModuleConfiguration.java`
- Create: `src/test/java/br/com/sea/solicitations/user/adapter/in/web/AdminUserControllerTest.java`
- Create: `src/test/java/br/com/sea/solicitations/user/adapter/in/web/dto/CreateInternalUserRequestTest.java`

**Interfaces:**
- Consumes: `CreateInternalUserUseCase` da Task 1 e `/admin/**` protegido por `SecurityConfiguration`.
- Produces: `POST /admin/users`, resposta `201` e JSON sem senha.

- [x] Escrever testes falhando para contrato JSON, validacao, senha redigida e acesso ADMIN/CLIENT.
- [x] Executar `./mvnw -Dtest=AdminUserControllerTest,CreateInternalUserRequestTest test` e confirmar RED.
- [x] Implementar DTOs, controller e wiring minimos.
- [x] Reexecutar e confirmar GREEN.

### Task 3: Persistencia real da cobertura

**Files:**
- Create: `src/test/java/br/com/sea/solicitations/user/adapter/out/persistence/InternalUserPersistenceIntegrationTest.java`
- Create: `scripts/test-internal-user-persistence.sh`

**Interfaces:**
- Consumes: `CreateInternalUserService`, `JpaUserRepositoryAdapter`, `BCryptPasswordHasher`, Flyway e PostgreSQL.
- Produces: comprovacao executavel de usuario, hash e linhas de `analyst_coverages`.

- [x] Escrever teste de integracao opt-in para analista com duas UFs e administrador sem cobertura.
- [x] Executar o teste contra PostgreSQL descartavel e confirmar que ele detecta a ausencia de cobertura persistida.
- [x] Executar novamente com o fluxo completo e confirmar GREEN.

### Task 4: Documentacao e verificacao

**Files:**
- Modify: `README.md`
- Modify: `index.md` somente se o mapa estrutural mudar.

**Interfaces:**
- Consumes: contrato concluido nas Tasks 1 a 3.
- Produces: exemplo de uso, regras de papel/cobertura e comando do teste real.

- [x] Atualizar README.
- [x] Executar `./mvnw test`.
- [x] Executar `git diff --check` e validar sintaxe dos scripts.
