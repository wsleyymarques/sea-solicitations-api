# Solicitations API

[![CI](https://github.com/wsleyymarques/sea-solicitations-api/actions/workflows/ci.yml/badge.svg)](https://github.com/wsleyymarques/sea-solicitations-api/actions/workflows/ci.yml)

API REST do desafio backend para gerenciamento de solicitacoes de atendimento.

## Stack inicial

- Java 21
- Spring Boot 3.5
- Maven
- PostgreSQL e Flyway
- Elasticsearch
- Spring Security e JWT
- Micrometer e Prometheus
- OpenAPI/Swagger

## Arquitetura

O projeto segue um monolito modular com arquitetura hexagonal pragmatica, organizado primeiro por funcionalidade:

```text
br.com.sea.solicitations
|-- user/
|   |-- domain/
|   |-- application/
|   |   |-- port/
|   |   |   |-- in/
|   |   |   `-- out/
|   |   `-- service/
|   `-- adapter/
|       |-- in/web/
|       |-- out/
|       `-- config/
|-- solicitation/
|   |-- domain/
|   |-- application/
|   `-- adapter/
|-- audit/
|   |-- domain/
|   |-- application/
|   `-- adapter/
|-- outbox/
|   |-- domain/
|   |-- application/
|   `-- adapter/
`-- shared/
    |-- domain/
    `-- adapter/
```

Os modulos `user` e `solicitation` possuem fluxos verticais completos. Em
`solicitation`, os casos de uso dependem de portas proprias e nao conhecem JPA,
HTTP nem ViaCEP; os adapters implementam essas fronteiras. `outbox` processa
pedidos duraveis de indexacao. `audit` registra operacoes de escrita por AOP. Cada modulo
recebe pacotes `application` e `adapter` somente quando existem casos de uso,
portas ou adapters reais, evitando abstracoes e pastas vazias criadas apenas
por convencao.

A linguagem do negocio esta em [CONTEXT.md](CONTEXT.md). O projeto evolui em
marcos funcionais sem datas fixas, conforme o
[roteiro de estudo e implementacao](docs/superpowers/specs/2026-10-28-study-implementation-roadmap-design.md).

## Executando os testes

```bash
./mvnw test
```

A consulta JPA do analista possui uma verificacao adicional contra PostgreSQL
real. O cadastro de usuarios internos tambem verifica usuario, hash BCrypt e
cobertura por UF no banco. Os comandos criam e removem containers descartaveis
e exigem Docker em execucao:

```bash
bash scripts/test-analyst-solicitation-query.sh
bash scripts/test-internal-user-persistence.sh
bash scripts/test-outbox-persistence.sh
bash scripts/test-audit-persistence.sh
```

Os testes HTTP com Testcontainers rodam em um perfil separado. Eles conferem
cadastro, login, autorizacao, criacao, endereco com consulta de CEP simulada,
confirmacao, submissao, listagem, aprovacao, rejeicao, Flyway, outbox e
auditoria no PostgreSQL real:

```bash
./mvnw -Pintegration verify
```

Esse comando exige Docker. `./mvnw verify` continua executavel sem Docker.
Para verificar tambem a inicializacao da aplicacao, PostgreSQL e Elasticsearch
pelo Compose e aguardar a indexacao real via outbox, com recursos temporarios
isolados:

```bash
bash scripts/smoke-compose.sh
```

## Integracao continua

O workflow [CI](.github/workflows/ci.yml) e executado em todo `push` e
`pull_request`. O job `verify` configura o Java 21, reutiliza o cache Maven e
executa, nesta ordem:

1. `./mvnw --batch-mode --no-transfer-progress verify`;
2. `npm ci && npm run build` em `frontend/`;
3. `docker compose --env-file .env.example config --quiet`;
4. `docker build --tag sea-solicitations-api:ci .`;
5. `./mvnw --batch-mode --no-transfer-progress -Pintegration verify`;
6. `bash scripts/smoke-compose.sh`.

Quando a pipeline falha, ela tenta disponibilizar os relatorios Surefire e Failsafe como
artifact por sete dias, sem transformar a ausencia de relatorios em uma nova
falha. As actions oficiais sao fixadas por SHA e o `GITHUB_TOKEN` possui apenas
permissao de leitura do conteudo do repositorio.

Depois que o workflow estiver na branch `main`, um ruleset do GitHub pode exigir
o status do job `verify` antes de permitir o merge. A configuracao fica em
`Settings > Rules > Rulesets` e transforma o CI em uma barreira efetiva contra
codigo que nao compila, testes quebrados ou imagens Docker que nao constroem.

## Ambiente com Docker Compose

Crie o arquivo local de variaveis de ambiente:

```bash
cp .env.example .env
openssl rand -base64 32
```

Copie o valor gerado para `JWT_SECRET` no `.env`. A aplicacao exige uma chave
Base64 que decodifique para pelo menos 32 bytes e falha na inicializacao quando
a configuracao e insegura.

Suba a aplicacao, o PostgreSQL e o Elasticsearch:

```bash
docker compose up --build --detach
```

Para executar somente a aplicação pelo terminal, mantendo PostgreSQL e
Elasticsearch no Docker:

```bash
docker compose up --detach postgres elasticsearch
./mvnw spring-boot:run
```

O Spring Boot importa automaticamente o arquivo `.env` da raiz do projeto.
Execute o comando Maven a partir dessa pasta.

Confirme que o terminal está usando o JDK 21:

```bash
java -version
./mvnw --version
```

No macOS com o OpenJDK 21 instalado pelo Homebrew, selecione-o na sessão atual
antes de executar o projeto:

```bash
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

O build rejeita outras versões de Java para evitar que a aplicação seja
executada acidentalmente em uma JVM diferente da exigida pelo desafio.

Verifique o estado dos containers:

```bash
docker compose ps
```

Os servicos ficarao disponiveis em:

- API: `http://localhost:8080`
- Healthcheck da API: `http://localhost:8080/actuator/health`
- PostgreSQL: `localhost:5432`
- Elasticsearch: `http://localhost:9200`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## Testando pela tela e pelo Postman

Para carregar usuarios prontos para teste, primeiro inicie o PostgreSQL:

```bash
docker compose up --detach postgres
ALLOW_TEST_SEED=1 bash scripts/seed-test-users.sh
```

A seed e idempotente: cria os usuarios que ainda nao existem e atribui cobertura
SP/RJ ao analista de teste. Ela exige `ALLOW_TEST_SEED=1` porque todos os usuarios
compartilham a senha de demonstracao `StrongPassword@123`; execute somente em
banco local de teste. As credenciais sao:

| Perfil | E-mail |
| --- | --- |
| CLIENT | `cliente.teste@example.com` |
| ANALYST | `analista.teste@example.com` |
| ADMIN | `admin.teste@example.com` |

Com a API ativa na porta `8080`, inicie o frontend local (Node.js 22):

```bash
cd frontend
npm ci
npm run dev
```

Abra `http://localhost:5173`. A tela usa o proxy do Vite para chamar a API,
sem alterar o CORS do backend. Para outra porta da API, defina
`VITE_API_TARGET=http://localhost:PORTA` antes de `npm run dev`.
O painel cobre cadastro e login do cliente, criacao e envio da solicitacao,
login e cadastro de analista pelo administrador, lista, busca, inicio da
analise e decisao. Para entrar com os usuarios da seed, use o e-mail e a senha
`StrongPassword@123` no formulario do perfil correspondente. Os tokens ficam
apenas na memoria da aba e sao descartados
ao recarregar a pagina. O ID da solicitacao e preenchido automaticamente ao
criar; tambem pode ser colado manualmente.

Para testar no Postman, importe
[`postman/sea-solicitacoes.postman_collection.json`](postman/sea-solicitacoes.postman_collection.json)
e [`postman/sea-local.postman_environment.json`](postman/sea-local.postman_environment.json).
Selecione o ambiente local e preencha `adminEmail` e `adminPassword` com as
credenciais do primeiro administrador. Crie essa conta pelo
`scripts/bootstrap-admin.sh` se ainda nao existir. Execute a colecao na
ordem numerada. Ela gera e-mails de teste exclusivos, captura tokens e ID da
solicitacao automaticamente e verifica os codigos e estados principais,
incluindo aprovacao e rejeicao em pedidos diferentes.
A busca usa indexacao assincrona; execute novamente a requisicao 11 se o
registro ainda nao aparecer. O arquivo de ambiente nao contem senha real.
Para regenerar os arquivos Postman apos alterar os contratos, execute
`python3 scripts/generate-postman.py`.

## Autenticacao JWT

Cadastre um cliente:

```bash
curl --request POST http://localhost:8080/auth/register \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "Joao Souza",
    "email": "joao@example.com",
    "password": "StrongPassword@123"
  }'
```

Autentique o usuario:

```bash
curl --request POST http://localhost:8080/auth/login \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "joao@example.com",
    "password": "StrongPassword@123"
  }'
```

A resposta contem `accessToken`, `tokenType` e `expiresIn`. Nas rotas
protegidas, envie o token no cabecalho:

```http
Authorization: Bearer <accessToken>
```

O JWT e assinado com HS256 e contem o ID do usuario em `sub`, alem das claims
`role` e `coveredStates`. A aplicacao nao cria sessao no servidor.

### Primeiro administrador local

Depois de configurar `.env`, inicie o PostgreSQL e execute a seed local uma
unica vez, informando o e-mail desejado:

```bash
docker compose up --detach postgres
scripts/bootstrap-admin.sh admin@example.com
```

O script pede a senha duas vezes, gera um hash BCrypt com custo 12 usando
`htpasswd` e insere o primeiro usuario `ADMIN`. Nenhuma senha ou hash fixo fica
no repositorio. Se ja existir um administrador, a seed nao altera sua conta ou
senha. Se o e-mail pertencer a um cliente ou analista, a insercao falha.

O comando requer `docker` e `htpasswd` no computador local. Depois, use
`POST /auth/login` com esse e-mail e senha para obter um token de administrador.
O teste da seed pode ser executado sem tocar no banco do projeto com
`bash scripts/test-bootstrap-admin.sh`; ele cria e remove um PostgreSQL
descartavel no Docker.

### Cadastro de usuarios internos

Com um token de `ADMIN`, crie um analista e defina ao menos uma UF de
cobertura:

```bash
curl --request POST http://localhost:8080/admin/users \
  --header 'Authorization: Bearer <adminAccessToken>' \
  --header 'Content-Type: application/json' \
  --data '{
    "name": "Ana Silva",
    "email": "ana@example.com",
    "password": "StrongPassword@123",
    "role": "ANALYST",
    "coveredStates": ["SP", "RJ"]
  }'
```

A mesma rota cria outro administrador usando `"role": "ADMIN"` e omitindo
`coveredStates` ou enviando uma lista vazia. O papel `CLIENT` nao e aceito:
clientes continuam sendo cadastrados somente por `POST /auth/register`.
Analista sem cobertura e administrador com cobertura retornam `400`; e-mail
ja utilizado retorna `409`; tokens sem papel `ADMIN` recebem `403`.

A resposta `201 Created` contem `id`, `name`, `email`, `role` e
`coveredStates`. A senha e seu hash nunca sao devolvidos. O primeiro
administrador continua sendo criado pelo bootstrap local descrito acima.

## Solicitacoes

Crie a solicitacao com os dados basicos usando um token de `CLIENT`:

```bash
curl --request POST http://localhost:8080/solicitations \
  --header 'Authorization: Bearer <accessToken>' \
  --header 'Content-Type: application/json' \
  --data '{
    "serviceType": "INSTALLATION",
    "title": "Instalacao de equipamento",
    "description": "Solicito a instalacao de um novo equipamento no endereco informado."
  }'
```

Complete a etapa de endereco com o ID devolvido. O cliente fornece apenas CEP,
numero e complemento; logradouro, bairro, cidade e UF sao consultados no
ViaCEP e nao podem ser substituidos pelo corpo da requisicao:

```bash
curl --request PUT http://localhost:8080/solicitations/<solicitationId>/address \
  --header 'Authorization: Bearer <accessToken>' \
  --header 'Content-Type: application/json' \
  --data '{
    "postalCode": "01001-000",
    "number": "100",
    "complement": "Apto 12"
  }'
```

O endpoint responde `200 OK` com a solicitacao ainda em `DRAFT`, a etapa
`CONFIRMATION` e o endereco completo. Somente o cliente proprietario pode
alterar a solicitacao. CEP inexistente produz `422`; indisponibilidade do
provedor produz `503`, sem persistir um endereco parcial. Uma edicao concorrente
do mesmo rascunho produz `409`, evitando sobrescrever a versao mais recente.

A integracao usa, por padrao, `https://viacep.com.br`, timeout de conexao de
`2s` e timeout de leitura de `3s`. Os valores podem ser alterados com
`VIACEP_BASE_URL`, `VIACEP_CONNECT_TIMEOUT` e `VIACEP_READ_TIMEOUT`.

Depois do endereco, complete a confirmacao:

```bash
curl --request PUT http://localhost:8080/solicitations/<solicitationId>/confirmation \
  --header 'Authorization: Bearer <accessToken>' \
  --header 'Content-Type: application/json' \
  --data '{
    "priority": "HIGH",
    "preferredDate": "2099-01-01",
    "estimatedValue": 100.00,
    "termsAccepted": true
  }'
```

A data preferida deve ser hoje ou futura, os termos precisam ser aceitos e o
valor estimado nao pode ser negativo. Para prioridade `HIGH`, o valor e
obrigatorio e deve ser de pelo menos `100.00`; para `LOW` e `MEDIUM`, ele e
opcional. A resposta permanece em `DRAFT`, agora com `currentStep` igual a
`COMPLETED`. Enquanto nao houver submissao, o cliente proprietario pode editar
a confirmacao repetindo o `PUT`.

Quando as tres etapas estiverem completas, envie a solicitacao:

```bash
curl --request POST http://localhost:8080/solicitations/<solicitationId>/submit \
  --header 'Authorization: Bearer <accessToken>'
```

A submissao nao recebe corpo e responde `200 OK` com `status` igual a
`SUBMITTED`, `currentStep` igual a `COMPLETED` e o instante `submittedAt`.
Somente o cliente proprietario pode submeter o proprio rascunho. A API valida
novamente a data preferida no momento da submissao, pois ela pode ter se
tornado passada desde o preenchimento da confirmacao.

Uma solicitacao incompleta responde `409 INCOMPLETE_SOLICITATION` e informa em
`violations` quais etapas ainda faltam (`basicData`, `address` e/ou
`confirmation`). Tentar submeter novamente ou submeter um rascunho cuja data
preferida expirou responde `409 INVALID_SOLICITATION_STATE`. Atualizacoes
concorrentes tambem respondem `409`, sem sobrescrever a versao persistida.

## Inicio da analise

Liste as solicitacoes visiveis ao analista antes de iniciar uma analise:

```bash
curl --request GET 'http://localhost:8080/analyst/solicitations?page=0&size=20' \
  --header 'Authorization: Bearer <analystAccessToken>'
```

A resposta contem `content`, `page`, `size`, `totalElements` e `totalPages`,
ordenados por submissao mais recente. A consulta mostra solicitacoes
`SUBMITTED` nas UFs cobertas pelo analista e solicitacoes `IN_REVIEW` somente
quando estiverem atribuidas a ele. Rascunhos, solicitacoes terminais e analises
atribuidas a outro analista nao aparecem. `page` comeca em zero e `size` aceita
valores de `1` a `100`.

Para consultar todos os dados de uma solicitacao visivel, use o detalhe:

```bash
curl --request GET http://localhost:8080/analyst/solicitations/<solicitationId> \
  --header 'Authorization: Bearer <analystAccessToken>'
```

O detalhe inclui dados basicos, endereco, confirmacao e, quando existente, a
decisao. Uma solicitacao `SUBMITTED` fica disponivel aos analistas que cobrem a
UF; depois do inicio da analise, somente o analista atribuido pode consulta-la.
Rascunhos, UFs fora da cobertura e solicitacoes atribuidas a outro analista
respondem `403 SOLICITATION_REVIEW_ACCESS_DENIED`.

Um analista pode iniciar a analise de uma solicitacao submetida cuja UF esteja
em sua cobertura:

```bash
curl --request POST http://localhost:8080/analyst/solicitations/<solicitationId>/start \
  --header 'Authorization: Bearer <analystAccessToken>'
```

O endpoint nao recebe corpo. Responde `200 OK` com `id`, `status` igual a
`IN_REVIEW`, `assignedAnalystId` e `reviewStartedAt`. A cobertura vem da claim
`coveredStates` do JWT, emitida no login. Mudancas de cobertura passam a valer
quando o analista recebe um novo token; o prazo padrao do token e de uma hora.

Uma UF fora da cobertura ou uma claim ausente/invalida responde
`403 SOLICITATION_REVIEW_ACCESS_DENIED`. Uma solicitacao inexistente responde
`404 SOLICITATION_NOT_FOUND`. Uma solicitacao que nao esteja em `SUBMITTED`
responde `409 INVALID_SOLICITATION_STATE`; atualizacoes concorrentes respondem
`409 SOLICITATION_CONCURRENT_UPDATE`.

## Decisao da analise

O analista que iniciou a analise pode aprovar ou rejeitar a solicitacao:

```bash
curl --request POST http://localhost:8080/analyst/solicitations/<solicitationId>/decide \
  --header 'Authorization: Bearer <analystAccessToken>' \
  --header 'Content-Type: application/json' \
  --data '{"outcome":"APPROVE","comment":"Parecer favoravel para atendimento."}'
```

Para rejeitar, envie `"outcome":"REJECT"`. O comentario e obrigatorio e deve
ter de 10 a 1000 caracteres depois de remover espacos nas extremidades. A
resposta `200 OK` contem `id`, `status`, `analystId`, `comment` normalizado e
`decidedAt`. Esses dados ficam persistidos com a solicitacao, permitindo
identificar quem decidiu, o parecer e quando ocorreu. A auditoria por AOP
registra tambem ator, resultado e request id da operacao.

Somente o analista atribuido, com cobertura da UF da solicitacao, pode decidir
enquanto ela esta em `IN_REVIEW`. A decisao e terminal: uma segunda tentativa
responde `409 INVALID_SOLICITATION_STATE`. Falta de cobertura ou atribuicao
responde `403 SOLICITATION_REVIEW_ACCESS_DENIED`; solicitacao inexistente,
`404 SOLICITATION_NOT_FOUND`; corpo invalido, `400`; atualizacao concorrente,
`409 SOLICITATION_CONCURRENT_UPDATE`.

`/decide` e a rota canonica do contrato. `/decision` continua aceito como alias
de compatibilidade para clientes antigos.

## Busca de solicitacoes no Elasticsearch

O analista pode pesquisar solicitacoes da sua cobertura:

```bash
curl --get http://localhost:8080/analyst/solicitations/search \
  --header 'Authorization: Bearer <analystAccessToken>' \
  --data-urlencode 'q=equipamento' \
  --data-urlencode 'status=SUBMITTED' \
  --data-urlencode 'state=SP' \
  --data-urlencode 'dateFrom=2026-11-01' \
  --data-urlencode 'dateTo=2026-11-30' \
  --data-urlencode 'sort=SUBMITTED_AT' \
  --data-urlencode 'page=0' \
  --data-urlencode 'size=20'
```

Filtros opcionais: `q` (titulo e descricao, ate 200 caracteres), `status`,
`serviceType`, `priority`, `state`, `dateFrom` e `dateTo`. As datas delimitam
dias completos de submissao em UTC. A ordenacao usa `sort`
(`SUBMITTED_AT`, `CREATED_AT` ou `PREFERRED_DATE`) e `direction` (`ASC` ou
`DESC`); o padrao e `SUBMITTED_AT DESC`. `page` comeca em zero, `size` vai de
`1` a `100`, e a janela solicitada deve caber nos primeiros 10.000 resultados.
A resposta inclui exatamente `items`, `page`, `size` e `total`.

`submittedFrom`, `submittedTo` e `sortBy` permanecem como aliases de
compatibilidade. Quando o nome canonico e o alias sao enviados juntos, o valor
canonico prevalece.

A cobertura vem sempre da claim `coveredStates` do JWT. `state` apenas
restringe o resultado. Solicitacoes `SUBMITTED` aparecem na cobertura;
`IN_REVIEW`, `APPROVED` e `REJECTED` aparecem somente ao analista atribuido.
Rascunhos nao aparecem. Cada resultado e conferido no PostgreSQL antes de ser
devolvido, evitando que um documento antigo exponha uma solicitacao cuja
atribuicao mudou. Se houver documentos atrasados, uma pagina pode ter menos
itens que `size`, e `total` ainda reflete a contagem do indice.

O indice `solicitations-v1` e criado com mapping na primeira operacao.
Criacao, edicao, submissao, inicio de analise e decisao gravam a solicitacao e
um evento de outbox na mesma transacao PostgreSQL. Um worker indexa o estado
atual em lotes de ate 20, a cada 5 segundos por padrao. A busca e eventualmente
consistente: escritas confirmadas podem demorar a aparecer no indice. Falhas do
Elasticsearch nao impedem a escrita; eventos pendentes sao repetidos com espera
exponencial de 5 segundos a 5 minutos. A API de busca responde
`503 SEARCH_UNAVAILABLE` se o Elasticsearch estiver indisponivel.

O poller pode ser desativado com `OUTBOX_POLLING_ENABLED=false`. O intervalo
e o atraso inicial aceitam `OUTBOX_POLL_INTERVAL_MS` e
`OUTBOX_INITIAL_DELAY_MS` (ambos 5000 por padrao). Para acompanhar pendencias:

```sql
SELECT count(*) AS pending, min(occurred_at) AS oldest_pending
FROM outbox_events WHERE processed_at IS NULL;
```

Solicitacoes anteriores a indexacao do Marco 8 precisam de reindexacao
especifica para aparecer na busca.

## Auditoria das operacoes

Os casos de uso de cadastro e de escrita de solicitacoes gravam registros em
`audit_logs`. Cada registro contem ator, acao, tipo e ID do recurso, resultado
(`SUCCESS` ou `FAILURE`), request id, metadados permitidos e instante. A
criacao de solicitacao gera tambem `BASIC_DATA_SAVED`; a criacao de analista
gera `ANALYST_STATES_ASSIGNED`. A decisao distingue aprovacao e rejeicao.

Envie `X-Request-Id` para correlacionar uma operacao com seu log. Se o header
estiver ausente ou invalido, a aplicacao gera um UUID. Os metadados nao incluem
senha, token, comentario, endereco nem corpo completo da requisicao. Falhas de
negocio sao gravadas em uma transacao propria, mesmo quando a transacao de
negocio reverte. Uma falha isolada de gravacao da auditoria aparece no log da
aplicacao e nao muda a resposta de negocio.

Para inspecionar os registros mais recentes no PostgreSQL:

```sql
SELECT occurred_at, actor_id, action, resource_type, resource_id,
       outcome, request_id, metadata
FROM audit_logs
ORDER BY occurred_at DESC
LIMIT 20;
```

## Metricas operacionais

O Actuator publica metricas Prometheus em `/actuator/prometheus`. A rota e
restrita ao perfil `ADMIN`; use um token de administrador para inspecao local:

```bash
curl http://localhost:8080/actuator/prometheus \
  --header 'Authorization: Bearer <accessToken>'
```

As metricas especificas sao `sea_outbox_pending` (eventos ainda nao
processados), `sea_outbox_processed_total` (eventos processados),
`sea_outbox_retries_total` (retries agendados) e
`sea_audit_write_failures_total` (falhas de gravacao da auditoria).
Nenhuma usa ID de usuario, email ou solicitacao como tag. Para monitoramento
externo, configure o acesso autenticado ao endpoint de forma deliberada.

Para acompanhar os logs:

```bash
docker compose logs --follow app
```

Para encerrar sem apagar os dados:

```bash
docker compose down
```

O comando `docker compose down --volumes` tambem remove todos os dados locais do PostgreSQL e do Elasticsearch.

## Migrations do banco

O Flyway executa automaticamente as migrations de `src/main/resources/db/migration` antes de o Hibernate validar o schema.

As migrations atuais sao:

```text
V1__create_initial_schema.sql
V2__outbox_retry_schedule.sql
```

Depois que uma migration for aplicada, ela nao deve ser editada. Mudancas futuras no banco devem ser adicionadas como `V3`, `V4` e assim por diante.
