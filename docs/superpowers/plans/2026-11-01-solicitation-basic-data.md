# Solicitation Basic Data Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expor o primeiro fluxo vertical de solicitacoes, permitindo que um cliente autenticado crie uma solicitacao `DRAFT` com os dados basicos e receba o progresso da proxima etapa.

**Architecture:** O adapter web obtem a identidade exclusivamente do `sub` do JWT e converte o JSON em `CreateSolicitationCommand`. O caso de uso cria o agregado, aplica `BasicData` e persiste pela porta `SolicitationRepository`; um adapter JPA implementa essa porta. A configuracao do modulo liga as dependencias sem acoplar o caso de uso ao mecanismo HTTP.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC, Spring Security Resource Server, Spring Data JPA, Jakarta Validation, JUnit 5, Mockito e MockMvc.

**Spec:** `docs/superpowers/specs/2026-10-28-study-implementation-roadmap-design.md`

## Global Constraints

- A aplicacao deve continuar usando Java 21 e a arquitetura hexagonal pragmatica existente.
- Somente um JWT com papel `CLIENT` pode acessar `/solicitations/**`.
- O `clientId` vem do `sub` do JWT e nunca do corpo da requisicao.
- A solicitacao nasce `DRAFT`; ao concluir dados basicos, `currentStep` passa a `ADDRESS`.
- Este incremento nao implementa endereco, ViaCEP, confirmacao, submissao ou Elasticsearch.

## Review Focus

- Corpo sem `serviceType`, titulo vazio ou descricao curta deve retornar `400` sem chamar o caso de uso.
- Um `serviceType` desconhecido deve usar a resposta JSON padronizada de corpo malformado.
- O identificador persistido deve ser devolvido com `status=DRAFT` e `currentStep=ADDRESS`.
- O cliente do comando deve ser exatamente o UUID do `sub` do JWT, sem aceitar ownership fornecido pelo corpo.
- Requisicoes sem autenticacao ou com papel diferente de `CLIENT` devem continuar bloqueadas pela configuracao de seguranca existente.

---

### Task 1: Proteger o caso de uso de criacao

**Files:**
- Create: `src/test/java/br/com/sea/solicitations/solicitation/application/service/CreateSolicitationServiceTest.java`
- Existing: `src/main/java/br/com/sea/solicitations/solicitation/application/service/CreateSolicitationService.java`

**Interfaces:**
- Consumes: `SolicitationRepository.save(Solicitation) -> UUID`.
- Produces: `CreateSolicitationUseCase.create(CreateSolicitationCommand) -> CreateSolicitationResult`.

- [x] **Step 1: Escrever o teste do comportamento existente**

Criar `createsDraftWithBasicDataAndReturnsNextStep`, capturando a entidade entregue a porta e verificando ownership, dados basicos, `DRAFT`, `ADDRESS` e o UUID retornado.

- [x] **Step 2: Executar o teste isolado**

Run: `./mvnw -Dtest=CreateSolicitationServiceTest test`

Expected: PASS para o caso de uso ja iniciado; qualquer falha revela uma lacuna antes da entrada HTTP.

### Task 2: Expor `POST /solicitations`

**Files:**
- Create: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/SolicitationControllerTest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/SolicitationController.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/CreateSolicitationRequest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/CreateSolicitationResponse.java`

**Interfaces:**
- Consumes: `CreateSolicitationUseCase` e o `sub` do `Jwt` autenticado.
- Produces: `POST /solicitations`, resposta `201 Created`, header `Location: /solicitations/{id}` e JSON com `id`, `status` e `currentStep`.

- [x] **Step 1: Escrever o teste HTTP que ainda falha**

Testar uma requisicao autenticada como `CLIENT`, capturar o comando, verificar que seu `clientId` vem do JWT e validar status, `Location` e JSON da resposta.

- [x] **Step 2: Confirmar RED**

Run: `./mvnw -Dtest=SolicitationControllerTest test`

Expected: FAIL de compilacao porque controller e DTOs ainda nao existem.

- [x] **Step 3: Implementar o contrato HTTP minimo**

Criar request com validacoes equivalentes ao dominio, response com factory `from`, e controller que converte `jwt.getSubject()` para UUID, cria o comando e chama o caso de uso.

- [x] **Step 4: Confirmar GREEN**

Run: `./mvnw -Dtest=SolicitationControllerTest test`

Expected: PASS.

- [x] **Step 5: Adicionar teste de validacao**

Enviar valores invalidos e verificar `400`, `VALIDATION_ERROR` e nenhuma interacao com o caso de uso.

- [x] **Step 6: Executar novamente o teste HTTP**

Run: `./mvnw -Dtest=SolicitationControllerTest test`

Expected: PASS.

### Task 3: Ligar o modulo ao Spring e verificar a regressao

**Files:**
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/config/SolicitationModuleConfiguration.java`
- Modify: `src/test/java/br/com/sea/solicitations/SolicitationsApiApplicationTests.java`

**Interfaces:**
- Consumes: bean da porta `SolicitationRepository` fornecido pelo adapter JPA.
- Produces: bean `CreateSolicitationUseCase` usado pelo controller.

- [x] **Step 1: Executar o teste de contexto antes da configuracao**

Run: `./mvnw -Dtest=SolicitationsApiApplicationTests test`

Expected: FAIL por ausencia de `CreateSolicitationUseCase` quando o controller for descoberto.

- [x] **Step 2: Registrar o caso de uso**

Criar `SolicitationModuleConfiguration` seguindo o padrao de `UserModuleConfiguration` e retornar `new CreateSolicitationService(solicitationRepository)`.

- [x] **Step 3: Verificar o contexto e toda a suite**

Run: `./mvnw test`

Expected: BUILD SUCCESS, zero falhas e zero erros.

- [x] **Step 4: Verificar qualidade do diff**

Run: `git diff --check && git status --short`

Expected: nenhum erro de whitespace; somente os arquivos deste incremento e o plano aparecem como alterados.
