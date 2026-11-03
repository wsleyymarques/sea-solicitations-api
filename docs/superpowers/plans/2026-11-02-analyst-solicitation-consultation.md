# Analyst Solicitation Consultation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expor uma consulta paginada que mostre ao analista somente solicitacoes disponiveis em sua cobertura ou ja atribuidas a ele.

**Architecture:** O controller obtem identidade e cobertura do JWT, o caso de uso mantem contratos independentes de Spring e o adapter JPA executa a regra de visibilidade no PostgreSQL. A resposta HTTP usa um resumo paginado e nao antecipa filtros do Elasticsearch.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC, Spring Data JPA, JUnit 5, Mockito e AssertJ.

**Spec:** `docs/superpowers/specs/2026-11-02-analyst-solicitation-consultation-design.md`

## Global Constraints

- Preservar arquitetura hexagonal pragmatica e nomes da linguagem de `CONTEXT.md`.
- Nao adicionar dependencia nem filtro avancado do Marco 8.
- Aplicar cobertura e atribuicao dentro da consulta persistente, nunca apenas em memoria.
- Usar `page` zero-based e limitar `size` ao intervalo de `1` a `100`.
- Preservar todas as alteracoes locais preexistentes e nao criar commits automaticos neste workspace sujo.

## Review Focus

- Cobertura vazia deve retornar pagina vazia sem consultar todas as UFs.
- Uma solicitacao `IN_REVIEW` de outro analista nunca pode aparecer.
- Paginacao negativa ou acima de 100 deve produzir `400`, nao `500`.
- Ordenacao deve ser estavel quando duas solicitacoes compartilham `submittedAt`.
- Claim `coveredStates` ausente, malformada ou com UF desconhecida deve continuar produzindo `403`.

---

### Task 1: Caso de uso de consulta

**Files:**
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/ListAnalystSolicitationsQuery.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/AnalystSolicitationSummary.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/ListAnalystSolicitationsResult.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/ListAnalystSolicitationsUseCase.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/out/SolicitationPage.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/application/port/out/SolicitationRepository.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/service/ListAnalystSolicitationsService.java`
- Test: `src/test/java/br/com/sea/solicitations/solicitation/application/service/ListAnalystSolicitationsServiceTest.java`

**Interfaces:**
- Consumes: `SolicitationRepository.findVisibleToAnalyst(UUID, Set<FederativeUnit>, int, int)`.
- Produces: `ListAnalystSolicitationsUseCase.list(ListAnalystSolicitationsQuery)` e resultados paginados imutaveis.

- [x] Escrever testes falhando para mapeamento de pagina, cobertura vazia e validacao de `page`/`size`.
- [x] Executar `./mvnw -Dtest=ListAnalystSolicitationsServiceTest test` e confirmar RED por tipos ausentes.
- [x] Implementar os contratos e o service minimos.
- [x] Reexecutar o teste e confirmar GREEN.

### Task 2: Consulta persistente e ordenacao

**Files:**
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/out/persistence/SpringDataSolicitationJpaRepository.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/out/persistence/JpaSolicitationRepositoryAdapter.java`
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/out/persistence/JpaSolicitationRepositoryAdapterTest.java`
- Create: `src/test/java/br/com/sea/solicitations/solicitation/adapter/out/persistence/SpringDataSolicitationJpaRepositoryIntegrationTest.java`
- Create: `scripts/test-analyst-solicitation-query.sh`

**Interfaces:**
- Consumes: a operacao de repository e `SolicitationPage` da Task 1.
- Produces: JPQL que combina cobertura com `SUBMITTED` ou `IN_REVIEW` atribuido, usando `PageRequest` ordenado por `submittedAt DESC, id DESC`.

- [x] Escrever testes falhando para parametros da consulta, ordenacao e cobertura vazia.
- [x] Executar `./mvnw -Dtest=JpaSolicitationRepositoryAdapterTest test` e confirmar RED.
- [x] Implementar a query Spring Data e o mapeamento da pagina.
- [x] Reexecutar o teste e confirmar GREEN.
- [x] Executar a query contra PostgreSQL descartavel e verificar cobertura, atribuicao, total e duas paginas.

### Task 3: Endpoint HTTP paginado

**Files:**
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/AnalystSolicitationSummaryResponse.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/ListAnalystSolicitationsResponse.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationController.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/config/SolicitationModuleConfiguration.java`
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/AnalystSolicitationControllerTest.java`

**Interfaces:**
- Consumes: `ListAnalystSolicitationsUseCase` da Task 1.
- Produces: `GET /analyst/solicitations?page=0&size=20` com resposta paginada e validacao HTTP.

- [x] Escrever testes falhando para resposta, defaults, RBAC, claim invalida e paginacao invalida.
- [x] Executar `./mvnw -Dtest=AnalystSolicitationControllerTest test` e confirmar RED.
- [x] Implementar DTOs, endpoint e wiring minimos.
- [x] Reexecutar o teste e confirmar GREEN.

### Task 4: Documentacao e regressao

**Files:**
- Modify: `README.md`
- Modify: `index.md` apenas se o mapa estrutural precisar de ajuste.

**Interfaces:**
- Consumes: contrato HTTP concluido nas Tasks 1 a 3.
- Produces: exemplo de uso e regras de visibilidade documentadas.

- [x] Atualizar README com consulta, paginacao e escopo de visibilidade.
- [x] Executar `./mvnw test` e confirmar todos os testes verdes.
- [x] Revisar `git diff --check` e `git status --short` sem alterar trabalho preexistente.
