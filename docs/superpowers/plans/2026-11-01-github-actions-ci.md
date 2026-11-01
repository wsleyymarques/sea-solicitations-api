# GitHub Actions CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar uma pipeline de integracao continua que valide Maven, Docker Compose e a imagem Docker em todo push e pull request.

**Architecture:** Um unico workflow GitHub Actions executa um job sequencial chamado `verify`, com Java 21, cache Maven, permissoes minimas, controle de concorrencia e upload condicional dos relatorios de teste. O README documenta o comportamento e como o job podera proteger a branch `main`.

**Tech Stack:** GitHub Actions, Eclipse Temurin 21, Maven Wrapper, Docker e Docker Compose.

**Spec:** `docs/superpowers/specs/2026-11-01-github-actions-ci-design.md`

## Global Constraints

- Executar em todo `push` e `pull_request`.
- Nao publicar imagens, pacotes ou fazer deploy.
- Nao adicionar secrets de producao.
- Usar actions oficiais fixadas por SHA imutavel, com a versao legivel em comentario.
- Conceder ao `GITHUB_TOKEN` somente `contents: read`.
- Manter o job nomeado `verify`.
- Preservar integralmente as alteracoes de solicitacoes existentes no checkout principal.

## Review Focus

- Um pull request com testes falhando deve terminar com o job vermelho e ainda tentar disponibilizar os relatorios Surefire.
- A ausencia de `target/surefire-reports` nao deve transformar uma falha anterior em outra falha de upload.
- O Compose deve ser validado sem depender do `.env` local ou de secrets do GitHub.
- Dois commits rapidos na mesma referencia devem cancelar a execucao mais antiga, sem cancelar pipelines de outras branches.
- O workflow deve conseguir executar o Maven Wrapper preservado como executavel pelo Git.

---

### Task 1: Workflow de integracao continua

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: `pom.xml`, `mvnw`, `.env.example`, `compose.yaml` e `Dockerfile` existentes.
- Produces: required status check `CI / verify` para pushes e pull requests.

- [x] **Step 1: Confirmar que o workflow ainda nao existe**

Run: `test ! -e .github/workflows/ci.yml`

Expected: codigo de saida `0`.

- [x] **Step 2: Resolver as versoes oficiais e seus SHAs imutaveis**

Consultar os releases atuais de `actions/checkout`, `actions/setup-java` e `actions/upload-artifact` com `gh api`, registrando a tag e o SHA de cada release usado.

Expected: tres SHAs completos de 40 caracteres e tags estaveis oficiais.

- [x] **Step 3: Criar `.github/workflows/ci.yml`**

Definir `name: CI`, gatilhos `push` e `pull_request`, `permissions.contents: read`, concorrencia por `${{ github.workflow }}-${{ github.ref }}` com cancelamento, e o job `verify` em `ubuntu-latest` com timeout de 30 minutos.

Adicionar steps para checkout, Temurin 21 com cache Maven, Maven `verify`, Compose `config --quiet`, Docker `build` e upload dos relatorios Surefire sob `if: failure()`, `if-no-files-found: ignore` e retencao de 7 dias.

- [x] **Step 4: Validar a sintaxe e as expressoes do workflow**

Baixar o `actionlint` oficial `v1.7.12` para um diretorio temporario, usando o asset `actionlint_1.7.12_darwin_arm64.tar.gz` do release `rhysd/actionlint`, e executar:

Run: `"$actionlint_dir/actionlint" .github/workflows/ci.yml`

Expected: codigo de saida `0`, sem diagnosticos.

- [x] **Step 5: Validar formatacao do diff**

Run: `git diff --check`

Expected: codigo de saida `0`.

### Task 2: Verificacao local e documentacao operacional

**Files:**
- Modify: `README.md`
- Verify: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: job `CI / verify` produzido pela Task 1.
- Produces: documentacao do CI e evidencia de que cada comando do runner funciona no projeto.

- [x] **Step 1: Executar a verificacao Maven com Java 21**

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 PATH=/opt/homebrew/opt/openjdk@21/bin:$PATH ./mvnw --batch-mode --no-transfer-progress verify`

Expected: `BUILD SUCCESS`, zero falhas e zero erros.

- [x] **Step 2: Validar o Docker Compose sem `.env` local**

Run: `docker compose --env-file .env.example config --quiet`

Expected: codigo de saida `0`.

- [x] **Step 3: Construir a imagem Docker**

Run: `docker build --tag sea-solicitations-api:ci .`

Expected: codigo de saida `0` e imagem local criada, sem publicacao.

- [x] **Step 4: Documentar o CI no README**

Adicionar o badge do workflow e uma secao `Integracao continua` explicando os gatilhos, as tres verificacoes, o artifact de falha e a futura exigencia de `CI / verify` por um ruleset da `main`.

- [x] **Step 5: Fazer a verificacao final**

Run: `git diff --check && "$actionlint_dir/actionlint" .github/workflows/ci.yml && git status --short`

Expected: nenhuma falha de formatacao ou workflow e somente arquivos deste plano modificados.

- [x] **Step 6: Criar o commit**

```bash
git add \
  .github/workflows/ci.yml \
  README.md \
  docs/superpowers/specs/2026-11-01-github-actions-ci-design.md \
  docs/superpowers/plans/2026-11-01-github-actions-ci.md
git commit -m "ci: add GitHub Actions verification workflow"
```
