# Flyer Mandatory Contract Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Match the flyer's mandatory search and decision HTTP contracts exactly.

**Architecture:** Keep application and persistence contracts stable where possible and adapt the public web boundary. Rename decision input actions in the application port because they model commands, while terminal domain statuses remain unchanged.

**Tech Stack:** Java 21, Spring Boot MVC, Jackson, JUnit 5, MockMvc, Maven, TypeScript/Vite.

**Spec:** `docs/superpowers/specs/2026-11-03-flyer-mandatory-contract-alignment-design.md`

## Global Constraints

- Search request parameter is `q`.
- Search response contains exactly `items`, `page`, `size`, and `total`.
- Decision input values are `APPROVE` and `REJECT`.
- Terminal solicitation statuses remain `APPROVED` and `REJECTED`.
- Do not add inferred endpoints or optional features.

## Review Focus

- `q` must reach the application query instead of being silently ignored.
- The search JSON must not expose legacy response properties.
- `APPROVE` must approve and `REJECT` must reject.
- Invalid legacy decision values must produce the standard malformed JSON response.
- README, frontend, and Postman must use only the flyer contract.

---

### Task 1: Flyer search request and response contract

**Files:**
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationControllerTest.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationController.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/SearchSolicitationsResponse.java`

**Interfaces:**
- Consumes: `SearchSolicitationsUseCase.search(SearchSolicitationsQuery)`.
- Produces: HTTP query parameter `q`; JSON response `items`, `page`, `size`, `total`.

- [x] Write controller tests proving `q` reaches `SearchSolicitationsQuery.text()` and the JSON has only the four flyer properties.
- [x] Run the focused tests and verify failure against `text` and legacy response fields.
- [x] Implement the minimal controller and response DTO changes.
- [x] Run the focused tests and verify they pass.

### Task 2: Flyer decision commands

**Files:**
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationControllerTest.java`
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/application/service/DecideSolicitationServiceTest.java`
- Modify: `src/test/java/br/com/sea/solicitations/audit/adapter/in/aop/AuditLogFactoryTest.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/DecisionOutcome.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/application/service/DecideSolicitationService.java`
- Modify: `src/main/java/br/com/sea/solicitations/audit/adapter/in/aop/AuditLogFactory.java`

**Interfaces:**
- Consumes: decision JSON field `outcome`.
- Produces: command values `APPROVE` and `REJECT`; statuses remain `APPROVED` and `REJECTED`.

- [x] Change tests first to send `APPROVE` and `REJECT`, including rejection auditing and invalid legacy input.
- [x] Run focused tests and verify failure against the old enum.
- [x] Rename decision command values and update service/audit mappings.
- [x] Run focused tests and verify they pass.

### Task 3: End-to-end contract and consumer synchronization

**Files:**
- Modify: `src/test/java/br/com/sea/solicitations/integration/ApiFlowIT.java`
- Modify: `frontend/index.html`
- Modify: `frontend/src/main.ts`
- Modify: `README.md`
- Modify: `scripts/generate-postman.py`
- Regenerate: `postman/sea-solicitacoes.postman_collection.json`

**Interfaces:**
- Consumes: Tasks 1 and 2 public HTTP contract.
- Produces: integration coverage and synchronized human/manual clients.

- [x] Update the integration test first to assert `q`, exact search response fields, and `APPROVE/REJECT`.
- [x] Run the integration test and verify it fails before consumer/document updates are considered complete.
- [x] Update frontend, README, and Postman generator; regenerate the collection.
- [x] Run integration, frontend build, Postman drift check, full Maven verification, and Compose smoke verification.
