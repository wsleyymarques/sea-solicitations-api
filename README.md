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
|   `-- domain/
|-- audit/
|   `-- domain/
|-- outbox/
|   `-- domain/
`-- shared/
    |-- domain/
    `-- adapter/
```

Os modulos `user` e `solicitation` possuem fluxos verticais completos. Em
`solicitation`, os casos de uso dependem de portas proprias e nao conhecem JPA,
HTTP nem ViaCEP; os adapters implementam essas fronteiras. Os modulos `audit` e
`outbox` ainda possuem principalmente suas fundacoes de dominio. Cada modulo
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

## Integracao continua

O workflow [CI](.github/workflows/ci.yml) e executado em todo `push` e
`pull_request`. O job `verify` configura o Java 21, reutiliza o cache Maven e
executa, nesta ordem:

1. `./mvnw --batch-mode --no-transfer-progress verify`;
2. `docker compose --env-file .env.example config --quiet`;
3. `docker build --tag sea-solicitations-api:ci .`.

Quando a pipeline falha, ela tenta disponibilizar os relatorios Surefire como
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

A migration inicial e:

```text
V1__create_initial_schema.sql
```

Depois que uma migration for aplicada, ela nao deve ser editada. Mudancas futuras no banco devem ser adicionadas como `V2`, `V3` e assim por diante.
