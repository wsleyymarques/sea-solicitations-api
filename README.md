# Solicitations API

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
|   `-- domain/
|-- solicitation/
|   `-- domain/
|-- audit/
|   `-- domain/
|-- outbox/
|   `-- domain/
`-- shared/
    `-- domain/
```

Cada modulo recebera pacotes `application` e `adapter` somente quando existirem casos de uso, portas ou adapters reais. Isso evita interfaces e pastas vazias criadas apenas por convencao.

## Executando os testes

```bash
./mvnw test
```

## Ambiente com Docker Compose

Crie o arquivo local de variaveis de ambiente:

```bash
cp .env.example .env
```

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
