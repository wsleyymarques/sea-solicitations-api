# Solicitation Address ViaCEP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permitir que o cliente proprietario complete a etapa de endereco de uma solicitacao `DRAFT` usando dados obtidos do ViaCEP.

**Architecture:** O caso de uso carrega e autoriza o agregado, consulta a porta `PostalCodeLookup`, combina os dados externos com numero e complemento e persiste pela porta existente. Um adapter `RestClient` encapsula o contrato do ViaCEP; o controller conhece apenas comandos e resultados da aplicacao.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC, Spring Security, RestClient, Spring Data JPA, Jakarta Validation, JUnit 5, Mockito e MockRestServiceServer.

**Spec:** `docs/superpowers/specs/2026-11-01-solicitation-address-viacep-design.md`

## Global Constraints

- Somente `CLIENT` proprietario pode alterar a solicitacao.
- O `clientId` vem exclusivamente do `sub` do JWT.
- Rua, bairro, cidade e UF vem exclusivamente da porta de CEP.
- A solicitacao precisa estar `DRAFT` e ter dados basicos.
- Falha externa nao pode salvar endereco nem avancar a etapa.
- Nao adicionar retry, cache, circuit breaker ou cliente reativo neste marco.

## Review Focus

- CEP valido com `erro=true` deve produzir `422`, nunca endereco vazio.
- Resposta HTTP nao bem-sucedida ou falha de transporte deve produzir `503` e nenhuma persistencia.
- Outro cliente nao pode usar o endpoint para consultar ou alterar a solicitacao.
- Solicitacao fora de `DRAFT` nao pode disparar chamada ao ViaCEP.
- Dados de rua, bairro, cidade e UF enviados no JSON nao podem substituir o ViaCEP.

---

### Task 1: Caso de uso e persistencia

**Files:**
- Create: `src/test/java/br/com/sea/solicitations/solicitation/application/service/CompleteSolicitationAddressServiceTest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/CompleteSolicitationAddressCommand.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/CompleteSolicitationAddressResult.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/in/CompleteSolicitationAddressUseCase.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/out/PostalCodeData.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/port/out/PostalCodeLookup.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/exception/*.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/application/service/CompleteSolicitationAddressService.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/application/port/out/SolicitationRepository.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/out/persistence/JpaSolicitationRepositoryAdapter.java`

**Interfaces:**
- Consumes: `SolicitationRepository.findById(UUID)`, `PostalCodeLookup.find(String)` e `Solicitation.completeAddress(Address)`.
- Produces: `CompleteSolicitationAddressUseCase.complete(command) -> result`.

- [x] **Step 1: Escrever testes falhando do service**

Cobrir sucesso com endereco persistido, solicitacao inexistente, outro proprietario e estado nao editavel; nos erros, lookup e save nao devem ocorrer.

- [x] **Step 2: Confirmar RED**

Run: `./mvnw -Dtest=CompleteSolicitationAddressServiceTest test`

Expected: FAIL porque os contratos e o service ainda nao existem.

- [x] **Step 3: Implementar portas, erros, service e leitura JPA**

O service valida existencia, ownership e estado antes de consultar CEP; cria `Address`, chama o agregado e salva.

- [x] **Step 4: Confirmar GREEN**

Run: `./mvnw -Dtest=CompleteSolicitationAddressServiceTest test`

Expected: PASS.

### Task 2: Adapter ViaCEP

**Files:**
- Create: `src/test/java/br/com/sea/solicitations/solicitation/adapter/out/viacep/ViaCepPostalCodeAdapterTest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/out/viacep/ViaCepResponse.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/out/viacep/ViaCepPostalCodeAdapter.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/config/ViaCepProperties.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/config/ViaCepConfiguration.java`
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Consumes: `GET /ws/{postalCode}/json/`.
- Produces: adapter de `PostalCodeLookup`, com `PostalCodeNotFoundException` ou `PostalCodeServiceUnavailableException`.

- [x] **Step 1: Escrever teste falhando do adapter**

Usar `MockRestServiceServer` para verificar URL, mapear resposta completa e tratar `erro=true` e 5xx.

- [x] **Step 2: Confirmar RED**

Run: `./mvnw -Dtest=ViaCepPostalCodeAdapterTest test`

Expected: FAIL porque o adapter nao existe.

- [x] **Step 3: Implementar adapter e configuracao com timeout**

Usar `RestClient`, sem retry, base URL configuravel e timeouts positivos.

- [x] **Step 4: Confirmar GREEN**

Run: `./mvnw -Dtest=ViaCepPostalCodeAdapterTest test`

Expected: PASS.

### Task 3: Endpoint, erros JSON e documentacao

**Files:**
- Modify: `src/test/java/br/com/sea/solicitations/solicitation/adapter/in/web/SolicitationControllerTest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/CompleteSolicitationAddressRequest.java`
- Create: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/dto/CompleteSolicitationAddressResponse.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/in/web/SolicitationController.java`
- Modify: `src/main/java/br/com/sea/solicitations/solicitation/adapter/config/SolicitationModuleConfiguration.java`
- Modify: `src/main/java/br/com/sea/solicitations/shared/adapter/in/web/error/GlobalExceptionHandler.java`
- Modify: `src/test/java/br/com/sea/solicitations/SolicitationsApiApplicationTests.java`
- Modify: `README.md`
- Modify: `RESOURCES.md`

**Interfaces:**
- Consumes: `CompleteSolicitationAddressUseCase` e `Jwt.sub`.
- Produces: `PUT /solicitations/{id}/address`, `200 OK`, endereco persistido e erros JSON padronizados.

- [x] **Step 1: Escrever testes HTTP falhando**

Cobrir sucesso, entrada invalida, `POSTAL_CODE_NOT_FOUND` e `POSTAL_CODE_SERVICE_UNAVAILABLE`.

- [x] **Step 2: Confirmar RED**

Run: `./mvnw -Dtest=SolicitationControllerTest test`

Expected: FAIL porque o endpoint e os DTOs ainda nao existem.

- [x] **Step 3: Implementar endpoint, handlers e wiring**

O controller cria o comando com o JWT; handlers traduzem os erros esperados da aplicacao; configuracao registra o caso de uso.

- [x] **Step 4: Confirmar GREEN e regressao**

Run: `./mvnw test`

Expected: BUILD SUCCESS, zero falhas e zero erros.

- [x] **Step 5: Atualizar documentacao e verificar diff**

Documentar endpoint, variaveis do ViaCEP e fontes oficiais; executar `git diff --check`.
