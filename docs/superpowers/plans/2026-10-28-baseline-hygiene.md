# Baseline e Higiene do Projeto — Plano de Implementacao

> **Para trabalhadores agenticos:** SUB-SKILL OBRIGATORIA: Use `superpowers:subagent-driven-development` (recomendado) ou `superpowers:executing-plans` para implementar este plano tarefa por tarefa. Os passos usam checkboxes (`- [ ]`) para acompanhamento.

**Objetivo:** Transformar o estado atual do projeto em um baseline reproduzivel, documentado, verificado e seguro no Git antes da implementacao das solicitacoes.

**Arquitetura:** Este marco nao adiciona regras de negocio. Ele estabiliza o fluxo vertical ja existente de cadastro e autenticacao, corrige a configuracao local e registra a arquitetura atual para que os proximos marcos partam de um ponto conhecido.

**Stack:** Java 21, Spring Boot 3.5, Maven, PostgreSQL, Elasticsearch, Docker Compose, Spring Security, JWT, JUnit e Git.

**Especificacao:** `docs/superpowers/specs/2026-10-28-study-implementation-roadmap-design.md`

## Restricoes globais

- Nao adicionar novas funcionalidades de negocio neste marco.
- Nao registrar `.env`, senhas, tokens ou segredos no Git.
- `JWT_SECRET` deve ser Base64 e decodificar para pelo menos 32 bytes.
- Executar Maven com JDK 21.
- Preservar todas as alteracoes existentes do usuario.
- Nao usar `git reset`, `git checkout --`, `git clean` ou outro comando destrutivo.
- O marco so termina com testes, Compose e smoke test verificados.

## Foco da revisao

- Segredo JWT ausente, Base64 malformado ou menor que 32 bytes deve impedir a inicializacao de forma clara.
- `.env` deve continuar ignorado, enquanto `.env.example` permanece versionado.
- README deve descrever a estrutura que realmente existe, sem apresentar modulos futuros como concluidos.
- O ambiente completo deve iniciar com as migrations e expor health, cadastro e login.
- O commit nao pode incluir segredo local, artefatos de build ou arquivos externos ao escopo.

---

### Tarefa 1: Corrigir a configuracao JWT local

**Arquivos:**
- Modificar localmente, sem versionar: `.env`
- Conferir: `.env.example`
- Conferir: `.gitignore`
- Testar: `src/test/java/br/com/sea/solicitations/shared/adapter/config/JwtConfigurationTest.java`

**Interfaces:**
- Consome: propriedade `security.jwt.secret` de `application.yml`.
- Produz: ambiente local capaz de construir `SecretKey` HS256 com no minimo 256 bits.

- [x] **Passo 1: Confirmar que `.env` esta ignorado**

Executar: `git check-ignore -v .env`

Esperado: uma regra de `.gitignore` apontando para `.env`.

- [x] **Passo 2: Gerar um segredo local forte**

Executar: `openssl rand -base64 32`

Esperado: uma nova string Base64. O valor nao deve ser copiado para logs, documentacao ou mensagens.

- [x] **Passo 3: Substituir somente `JWT_SECRET` no `.env`**

Usar o valor gerado no passo anterior e manter as demais variaveis locais. Nao alterar `.env.example` para conter o segredo real.

- [x] **Passo 4: Verificar o tamanho sem revelar o segredo**

Executar uma verificacao local que leia `JWT_SECRET`, decodifique em memoria e confirme `decoded bytes >= 32`, imprimindo apenas o tamanho.

Esperado: pelo menos `32` bytes.

- [x] **Passo 5: Executar os testes da configuracao JWT**

Executar: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./mvnw -Dtest=JwtPropertiesTest,JwtConfigurationTest test`

Esperado: testes aprovados, incluindo segredo valido, Base64 malformado, chave curta e redacao no `toString()`.

### Tarefa 2: Alinhar a documentacao e os registros Teach

**Arquivos:**
- Modificar: `README.md:16-34`
- Revisar: `MISSION.md`
- Revisar: `RESOURCES.md`
- Revisar: `NOTES.md`
- Conferir: `CONTEXT.md`
- Conferir: `docs/superpowers/specs/2026-10-28-study-implementation-roadmap-design.md`

**Interfaces:**
- Consome: estrutura atual dos pacotes e o desenho aprovado.
- Produz: mapa arquitetural verdadeiro e um formato repetivel para registrar aprendizado.

- [x] **Passo 1: Atualizar a arvore arquitetural do README**

Substituir a arvore simplificada por uma arvore que mostre:

- `user/domain`, `user/application` e `user/adapter` como fluxo implementado;
- `solicitation/domain`, `audit/domain` e `outbox/domain` como fundacoes ainda sem fluxo completo;
- `shared/domain` e `shared/adapter` com responsabilidades compartilhadas reais.

Nao marcar Elasticsearch, auditoria AOP ou solicitacoes HTTP como concluidos.

- [x] **Passo 2: Adicionar links para contexto e roteiro**

No README, incluir links relativos para `CONTEXT.md` e para o desenho do roteiro. Explicar em uma frase que a evolucao acontece em marcos funcionais, sem datas fixas.

- [x] **Passo 3: Revisar os registros da jornada**

Confirmar que `MISSION.md` descreve o resultado profissional esperado, `RESOURCES.md` contem apenas fontes confiaveis e anotadas e `NOTES.md` mostra o objetivo e a proxima verificacao. Nao criar `learning-records/` enquanto o aluno nao demonstrar um aprendizado nao trivial.

- [x] **Passo 4: Verificar links e ausencia de segredos**

Executar: `test -f CONTEXT.md && test -f MISSION.md && test -f RESOURCES.md && test -f NOTES.md && test -f docs/superpowers/specs/2026-10-28-study-implementation-roadmap-design.md`

Executar: `git diff --check`

Esperado: ambos os comandos finalizam com codigo `0`.

### Tarefa 3: Verificar o baseline de codigo e infraestrutura

**Arquivos:**
- Verificar: `pom.xml`
- Verificar: `compose.yaml`
- Verificar: `Dockerfile`
- Verificar: `src/main/resources/application.yml`
- Verificar: `src/main/resources/db/migration/V1__create_initial_schema.sql`
- Verificar: `src/main/java/**`
- Verificar: `src/test/java/**`

**Interfaces:**
- Consome: implementacao atual de cadastro, login, JWT, PostgreSQL e Elasticsearch.
- Produz: evidencia fresca de compilacao, testes, configuracao do Compose e funcionamento minimo da API.

- [ ] **Passo 1: Confirmar o Java usado pelo Maven**

Executar: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./mvnw --version`

Esperado: Java `21`.

- [ ] **Passo 2: Executar toda a suite de testes**

Executar: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ./mvnw test`

Esperado: build bem-sucedido, sem falhas ou erros.

- [ ] **Passo 3: Validar a interpolacao do Compose**

Executar: `docker compose config --quiet`

Esperado: codigo de saida `0` e nenhuma variavel obrigatoria ausente.

- [ ] **Passo 4: Construir e iniciar o ambiente completo**

Executar: `docker compose up --build --detach`

Esperado: `app`, `postgres` e `elasticsearch` iniciados.

- [ ] **Passo 5: Aguardar e conferir saude**

Executar: `docker compose ps`

Executar: `curl --fail --silent http://localhost:8080/actuator/health`

Esperado: containers saudaveis e resposta com `"status":"UP"`.

- [ ] **Passo 6: Executar smoke test de cadastro e login**

Cadastrar um e-mail local novo em `POST /auth/register`, confirmar `201`, depois autenticar o mesmo usuario em `POST /auth/login` e confirmar `200`, `tokenType=Bearer`, `expiresIn > 0` e token nao vazio. Nao imprimir o token completo no relatorio de aprendizado.

- [ ] **Passo 7: Registrar e demonstrar o aprendizado do marco**

Atualizar `NOTES.md` com a evidencia dos comandos e responder com palavras proprias pelo menos estas perguntas:

- Por que uma API JWT pode ser stateless?
- Por que usar uma porta para `PasswordHasher` e `AccessTokenGenerator`?
- Por que o segredo HS256 precisa de tamanho minimo?

Depois da revisao das respostas, criar `learning-records/0001-baseline-de-autenticacao.md` somente para os aprendizados efetivamente demonstrados.

### Tarefa 4: Criar um checkpoint seguro no Git

**Arquivos:**
- Adicionar: alteracoes de autenticacao, configuracao, testes e documentacao verificadas nas tarefas anteriores.
- Excluir: `.env`, `target/`, segredos e qualquer arquivo fora do escopo.

**Interfaces:**
- Consome: baseline integralmente verificado.
- Produz: historico limpo a partir do qual o Marco 1 pode ser desenvolvido.

- [ ] **Passo 1: Inspecionar o estado antes de preparar arquivos**

Executar: `git status --short`

Executar: `git diff --cached --stat`

Esperado: compreender os cinco arquivos parcialmente preparados e todos os arquivos ainda nao rastreados antes de alterar o indice.

- [ ] **Passo 2: Preparar integralmente o fluxo de autenticacao e sua infraestrutura**

Executar:

```bash
git add \
  .env.example \
  .gitignore \
  README.md \
  compose.yaml \
  src/main/resources/application.yml \
  src/main/java/br/com/sea/solicitations/shared/adapter \
  src/main/java/br/com/sea/solicitations/user/adapter \
  src/main/java/br/com/sea/solicitations/user/application \
  src/test/java/br/com/sea/solicitations/SolicitationsApiApplicationTests.java \
  src/test/java/br/com/sea/solicitations/shared \
  src/test/java/br/com/sea/solicitations/user
```

Isso prepara novamente os cinco arquivos com estado `AM`, fazendo o indice receber o conteudo completo atual. Nao adicionar `.env`.

- [ ] **Passo 3: Revisar exatamente o que entrara no commit de codigo**

Executar: `git diff --cached --check`

Executar: `git diff --cached --stat`

Executar: `git diff --cached --name-only`

Esperado: somente autenticacao, seguranca, configuracao, testes e documentacao operacional; nenhum segredo.

- [ ] **Passo 4: Criar o commit do fluxo atual**

Executar: `git commit -m "feat: add client registration and JWT authentication"`

- [ ] **Passo 5: Preparar e revisar a documentacao de planejamento e aprendizado**

Executar: `git add MISSION.md RESOURCES.md NOTES.md learning-records docs/superpowers/specs/2026-10-28-study-implementation-roadmap-design.md docs/superpowers/plans/2026-10-28-baseline-hygiene.md`

Executar: `git diff --cached --check && git diff --cached --stat`

Esperado: somente documentos sem segredos ou artefatos.

- [ ] **Passo 6: Criar o commit de documentacao**

Executar: `git commit -m "docs: add implementation study roadmap"`

- [ ] **Passo 7: Verificacao final do marco**

Executar: `git status --short`

Executar: `git log --oneline --decorate -3`

Esperado: arvore limpa para arquivos versionados, `.env` ausente do status e dois novos commits coerentes.

## Autorrevisao do plano

- O plano cobre segredo JWT, documentacao, testes, Compose, smoke test e checkpoint Git exigidos pelo Marco 0.
- Nenhuma funcionalidade de solicitacao, Elasticsearch ou auditoria foi antecipada.
- Os passos de Git preservam o trabalho existente e nao usam comandos destrutivos.
- O teste de integracao automatizado permanece no Marco 11; o smoke test deste marco apenas valida o ambiente atual.
- Os detalhes da chave e dos tokens permanecem locais e nao sao registrados.
