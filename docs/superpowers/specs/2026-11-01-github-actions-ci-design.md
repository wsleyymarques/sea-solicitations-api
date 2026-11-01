# Desenho do CI com GitHub Actions

## Intencao

Adicionar integracao continua ao repositorio para detectar rapidamente falhas de compilacao, testes, configuracao do Docker Compose e construcao da imagem Docker. A pipeline tambem deve servir como evidencia tecnica clara durante a avaliacao da vaga, sem simular uma entrega ou um deploy que o projeto ainda nao possui.

## Objetivos

- Executar em todo `push` e `pull_request`.
- Usar Ubuntu e Eclipse Temurin Java 21.
- Reaproveitar o cache de dependencias e do Maven Wrapper.
- Executar o ciclo Maven `verify` com o wrapper do projeto.
- Validar a interpolacao de `compose.yaml` usando `.env.example`.
- Construir a imagem definida pelo `Dockerfile`, sem publica-la.
- Disponibilizar os relatorios Surefire quando a execucao falhar.
- Cancelar execucoes antigas da mesma referencia quando chegar um commit mais novo.
- Aplicar o principio do menor privilegio ao `GITHUB_TOKEN`.
- Documentar o fluxo e a futura protecao da branch `main` no README.

## Nao objetivos

- Publicar imagens ou pacotes.
- Fazer deploy em qualquer ambiente.
- Executar smoke tests com a aplicacao, PostgreSQL e Elasticsearch.
- Adicionar secrets de producao.
- Configurar automaticamente regras externas do repositorio GitHub.

## Abordagem escolhida

Um unico workflow `.github/workflows/ci.yml` tera um job chamado `verify`. O job executara as verificacoes em sequencia para manter o resultado simples de compreender e para poder ser exigido futuramente por um ruleset da `main`.

O workflow usara actions oficiais com referencias imutaveis por SHA e comentarios indicando suas versoes legiveis. O `GITHUB_TOKEN` recebera somente `contents: read`. A concorrencia sera agrupada por workflow e referencia Git, cancelando uma execucao anterior da mesma branch ou pull request.

## Fluxo

1. Um `push` ou `pull_request` inicia o workflow.
2. O runner baixa o repositorio.
3. O runner configura Temurin 21 e o cache Maven.
4. `./mvnw --batch-mode --no-transfer-progress verify` compila, testa e empacota o projeto.
5. `docker compose --env-file .env.example config --quiet` valida a configuracao do ambiente.
6. `docker build --tag sea-solicitations-api:ci .` comprova que o Dockerfile gera uma imagem.
7. Se houver falha, os relatorios de `target/surefire-reports` sao enviados como artifact quando existirem.

## Limites operacionais

- Runner: `ubuntu-latest`.
- Java: Temurin `21`.
- Timeout do job: 30 minutos.
- Retencao dos relatorios de falha: 7 dias.
- O workflow nao depende do `.env` local nem registra credenciais.
- O job deve permanecer nomeado `verify` para poder ser usado como required status check.

## Criterios de conclusao

- O YAML e aceito por um validador de workflows.
- Maven `verify` passa com Java 21.
- Docker Compose e validado com `.env.example`.
- A imagem Docker e construida localmente.
- O README explica gatilhos, etapas e a relacao com a protecao da `main`.
- Nenhuma alteracao do modulo de solicitacoes em andamento entra no commit.
