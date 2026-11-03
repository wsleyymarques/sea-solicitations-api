# Flyer Contract Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the API accept every required flyer route and parameter while preserving current clients, reliably map concurrent JPA updates to HTTP 409, and restrict Prometheus metrics to administrators.

**Architecture:** Add a read-only analyst-detail use case that loads PostgreSQL as the source of truth and enforces coverage/assignment before mapping the aggregate to immutable response records. Keep compatibility at the HTTP adapter with route and query-parameter aliases. Force JPA flush inside the persistence adapter's optimistic-lock translation boundary, and enforce metrics authorization in the security filter chain.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC/Security/Data JPA, PostgreSQL/Testcontainers, Elasticsearch, JUnit 5, Mockito, Maven, TypeScript/Vite, Python Postman generator.

**Spec:** `docs/superpowers/specs/2026-11-03-flyer-contract-compatibility-design.md`

## Global Constraints

- Preserve Java 21 and the existing modular hexagonal package layout.
- Keep `/decision`, `submittedFrom`, `submittedTo`, and `sortBy` as backwards-compatible aliases.
- Treat PostgreSQL and JWT claims as the authorization source of truth; never accept analyst identity or coverage from request input.
- Keep solicitation and outbox writes in the same PostgreSQL transaction.
- Use the existing `ApiErrorResponse` contract for all errors.
- Do not change eventual-consistency semantics between PostgreSQL and Elasticsearch.

## Review Focus

- A `DRAFT`, out-of-coverage, or other-analyst solicitation must never be exposed by the detail endpoint; pin in Task 1 service and HTTP tests.
- Canonical and legacy query parameters sent together must choose the canonical value; pin in Task 2 controller tests.
- A real stale JPA entity must fail during the adapter-controlled flush and become the domain concurrency exception; pin in Task 3 persistence integration test.
- Prometheus must return 401 anonymously, 403 to CLIENT/ANALYST, and 200 to ADMIN; pin in Task 4 security and smoke tests.
- OpenAPI, frontend, Postman, and integration tests must exercise the flyer names rather than only the compatibility aliases; pin in Tasks 2 and 5.

---

### Task 1: Analyst solicitation detail

**Files:**
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/GetAnalystSolicitationQuery.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/GetAnalystSolicitationResult.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/GetAnalystSolicitationUseCase.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/service/GetAnalystSolicitationService.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/GetAnalystSolicitationResponse.java`
- Create: `src/test/java/br/com/sea/solicitations/solicitation/application/service/GetAnalystSolicitationServiceTest.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/config/SolicitationModuleConfiguration.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationController.java`
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationControllerTest.java`

**Interfaces:**
- Consumes: `SolicitationRepository.findById(UUID)` and JWT `sub`/`coveredStates`.
- Produces: `GetAnalystSolicitationUseCase.get(GetAnalystSolicitationQuery)` and `GET /analyst/solicitations/{id}`.

- [ ] **Step 1: Write failing application-service tests**

  Cover full mapping for `SUBMITTED`, assigned terminal visibility, `404` for a missing ID, and `403 SolicitationReviewAccessDeniedException` for `DRAFT`, uncovered UF, or a non-assigned analyst.

- [ ] **Step 2: Run the service test and verify RED**

  Run: `./mvnw -q -Dtest=GetAnalystSolicitationServiceTest test`
  Expected: compilation failure because the detail interfaces/service do not exist.

- [ ] **Step 3: Implement the query, result, use case, and service**

  `GetAnalystSolicitationQuery(UUID solicitationId, UUID analystId, Set<FederativeUnit> coveredStates)` defensively copies coverage. The result uses nested immutable records for basic data, address, confirmation, and optional decision, rather than exposing JPA entities.

- [ ] **Step 4: Run the service test and verify GREEN**

  Run: `./mvnw -q -Dtest=GetAnalystSolicitationServiceTest test`
  Expected: PASS.

- [ ] **Step 5: Write failing controller tests**

  Assert the complete response and JWT-derived query, plus 401, wrong-role 403, malformed coverage 403, missing 404, and denied 403.

- [ ] **Step 6: Run the controller test and verify RED**

  Run: `./mvnw -q -Dtest=AnalystSolicitationControllerTest test`
  Expected: the new GET returns 404 or the application context lacks the new mock bean.

- [ ] **Step 7: Wire the bean, response DTO, and GET mapping**

  Add the use case to `SolicitationModuleConfiguration` and the controller constructor. Map `GetAnalystSolicitationResult` through `GetAnalystSolicitationResponse.from(...)`.

- [ ] **Step 8: Run focused tests and commit**

  Run: `./mvnw -q -Dtest=GetAnalystSolicitationServiceTest,AnalystSolicitationControllerTest test`
  Expected: PASS.

  Commit: `feat: add analyst solicitation detail`

### Task 2: Flyer decision route and search parameter aliases

**Files:**
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationController.java`
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationControllerTest.java`
- Modify: `src/test/java/br/com/sea/solicitations/integration/ApiFlowIT.java`

**Interfaces:**
- Consumes: existing `DecideSolicitationUseCase` and `SearchSolicitationsUseCase`.
- Produces: canonical `/decide`, `dateFrom`, `dateTo`, and `sort`, with legacy aliases retained.

- [ ] **Step 1: Write failing controller contract tests**

  Add one test proving `/decide` invokes the existing decision command, one retaining `/decision`, one using canonical search names, and one sending both forms to assert canonical precedence.

- [ ] **Step 2: Run the controller test and verify RED**

  Run: `./mvnw -q -Dtest=AnalystSolicitationControllerTest test`
  Expected: `/decide` is 404 and canonical query values are absent/defaulted.

- [ ] **Step 3: Implement route and query aliases**

  Map the decision method with both paths. Accept nullable canonical and legacy parameters, resolve each pair with a small private canonical-first helper, then construct the unchanged `SearchSolicitationsQuery`; keep existing defaults when both sort names are absent.

- [ ] **Step 4: Run the controller test and verify GREEN**

  Run: `./mvnw -q -Dtest=AnalystSolicitationControllerTest test`
  Expected: PASS.

- [ ] **Step 5: Update the HTTP integration test to the flyer contract**

  Add the detail GET before/after assignment, switch the primary decision flow to `/decide`, add `/decide` and the detail GET to OpenAPI assertions, and query search using `dateFrom`, `dateTo`, and `sort`.

- [ ] **Step 6: Run focused integration verification and commit**

  Run: `./mvnw -q -Pintegration -Dit.test=ApiFlowIT verify`
  Expected: all ApiFlowIT tests PASS against PostgreSQL.

  Commit: `feat: align analyst API with flyer contract`

### Task 3: Reliable optimistic-lock translation

**Files:**
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/out/persistence/JpaSolicitationRepositoryAdapter.java`
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/out/persistence/JpaSolicitationRepositoryAdapterTest.java`
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/out/persistence/SpringDataSolicitationJpaRepositoryIntegrationTest.java`

**Interfaces:**
- Consumes: `JpaRepository.saveAndFlush(Solicitation)`.
- Produces: `SolicitationConcurrentUpdateException` while still inside the adapter catch boundary.

- [ ] **Step 1: Change the unit test first**

  Require `saveAndFlush` rather than `save`, and make `saveAndFlush` throw `ObjectOptimisticLockingFailureException`; assert the translated application exception.

- [ ] **Step 2: Run the adapter test and verify RED**

  Run: `./mvnw -q -Dtest=JpaSolicitationRepositoryAdapterTest test`
  Expected: verification failure because production still calls `save`.

- [ ] **Step 3: Implement the minimal persistence change**

  Call `solicitationJpaRepository.saveAndFlush(...)` inside the existing try/catch. Do not change the outbox decorator transaction.

- [ ] **Step 4: Run the adapter test and verify GREEN**

  Run: `./mvnw -q -Dtest=JpaSolicitationRepositoryAdapterTest test`
  Expected: PASS.

- [ ] **Step 5: Add a real PostgreSQL stale-version regression test**

  Load two detached copies in separate transactions, persist the first, and assert the adapter translates the second stale flush. Keep the test under the existing opt-in integration mechanism.

- [ ] **Step 6: Run the persistence integration test and commit**

  Run: `bash scripts/test-analyst-solicitation-query.sh`
  Expected: PASS including the stale-version scenario.

  Commit: `fix: translate optimistic lock conflicts on flush`

### Task 4: Restrict Prometheus to administrators

**Files:**
- Modify: `src/main/java/br/com/sea/solicitations/shared/adapter/in/web/security/SecurityConfiguration.java`
- Modify: `src/test/java/br/com/sea/solicitations/shared/adapter/in/web/security/SecurityConfigurationTest.java`
- Modify: `scripts/smoke-compose.sh`

**Interfaces:**
- Consumes: JWT role authority conversion.
- Produces: public health, ADMIN-only Prometheus, unchanged API security.

- [ ] **Step 1: Write failing security tests**

  Assert `/actuator/prometheus` returns 401 anonymously, 403 for CLIENT and ANALYST, and reaches the endpoint for ADMIN. Preserve public health assertions.

- [ ] **Step 2: Run the security test and verify RED**

  Run: `./mvnw -q -Dtest=SecurityConfigurationTest test`
  Expected: CLIENT/ANALYST are currently allowed past authorization.

- [ ] **Step 3: Add the ADMIN matcher**

  Place `/actuator/prometheus` ADMIN authorization before `.anyRequest().authenticated()`.

- [ ] **Step 4: Run the security test and verify GREEN**

  Run: `./mvnw -q -Dtest=SecurityConfigurationTest test`
  Expected: PASS.

- [ ] **Step 5: Update Compose smoke authentication**

  Seed a smoke-only ADMIN with a fixed test BCrypt hash in the disposable PostgreSQL container, log it in, assert CLIENT receives 403 on Prometheus, and use the ADMIN token for metric-content assertions.

- [ ] **Step 6: Run the smoke test and commit**

  Run: `bash scripts/smoke-compose.sh`
  Expected: health, OpenAPI, restricted metrics, registration, login, outbox, and Elasticsearch checks PASS.

  Commit: `fix: restrict prometheus metrics to administrators`

### Task 5: Synchronize clients and documentation

**Files:**
- Modify: `frontend/src/main.ts`
- Modify: `frontend/index.html`
- Modify: `scripts/generate-postman.py`
- Regenerate: `postman/sea-solicitacoes.postman_collection.json`
- Modify: `README.md`
- Modify: `docs/superpowers/specs/2026-11-02-solicitation-decision-design.md`
- Modify: `docs/superpowers/specs/2026-11-02-solicitation-elasticsearch-search-design.md`
- Modify: `docs/superpowers/specs/2026-11-02-analyst-solicitation-consultation-design.md`

**Interfaces:**
- Consumes: the canonical HTTP contracts from Tasks 1 and 2.
- Produces: examples and test clients that use flyer names by default.

- [ ] **Step 1: Update frontend contract usage**

  Switch decisions to `/decide`, add a detail action for the selected analyst solicitation, and name any search date/sort inputs `dateFrom`, `dateTo`, and `sort`.

- [ ] **Step 2: Update and regenerate Postman**

  Insert a detail request after listing, use flyer query names in the search request, and switch both decisions to `/decide` in `scripts/generate-postman.py`. Run: `python3 scripts/generate-postman.py`.

- [ ] **Step 3: Update README and historical design docs**

  Document the canonical names first, explicitly label old names as compatibility aliases, and remove statements that detail lookup is out of scope.

- [ ] **Step 4: Verify generated artifacts and frontend**

  Run: `npm run build` in `frontend/`.
  Run: `git diff --check`.
  Expected: both PASS and generated Postman JSON contains `/decide`, detail GET, `dateFrom`, `dateTo`, and `sort`.

- [ ] **Step 5: Commit**

  Commit: `docs: synchronize flyer API clients and examples`

### Task 6: Full regression verification

**Files:**
- Modify only if a verification failure exposes a defect covered by this spec; add a failing regression test before each correction.

**Interfaces:**
- Consumes: all prior task outputs.
- Produces: verified repository state ready for review.

- [ ] **Step 1: Run the complete unit/build lifecycle**

  Run: `./mvnw -q verify`
  Expected: 0 failures and 0 errors.

- [ ] **Step 2: Run PostgreSQL HTTP integration**

  Run: `./mvnw -q -Pintegration verify`
  Expected: 0 failures and 0 errors.

- [ ] **Step 3: Validate deployment artifacts**

  Run: `docker compose --env-file .env.example config --quiet`
  Run: `docker build --tag sea-solicitations-api:verification .`
  Expected: both exit 0.

- [ ] **Step 4: Run final consistency checks**

  Run: `npm run build` in `frontend/`.
  Run: `python3 scripts/generate-postman.py` followed by `git diff --exit-code -- postman/`.
  Run: `git diff --check` and `git status --short`.
  Expected: builds/checks PASS and only intentional changes remain.

- [ ] **Step 5: Request code review**

  Use `superpowers:requesting-code-review`, address findings with regression tests, rerun the relevant focused checks, then repeat the full verification before completion.
