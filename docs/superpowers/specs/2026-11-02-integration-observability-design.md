# Marco 11 — Integracao, documentacao e observabilidade

## Intencao

Demonstrar que a API inicia com Flyway e PostgreSQL reais, documentar o
contrato HTTP e tornar a outbox e a auditoria observaveis. O teste principal
percorre HTTP, autenticacao, persistencia, outbox e auditoria.

## Abordagens

- Testcontainers no perfil Maven `integration` (escolhida): sobe PostgreSQL
  descartavel com `@ServiceConnection` e executa um teste HTTP em porta
  aleatoria. O `verify` comum continua independente de Docker; a CI executa
  o perfil em job proprio.
- Scripts Docker existentes: uteis para verificacoes isoladas, mas nao
  exercitam todo o caminho HTTP no mesmo teste.

## Contrato

- Teste de integracao cadastra cliente, autentica, cria solicitacao, verifica
  resposta de erro e permissao, Flyway V1/V2, solicitacao, outbox e auditoria
  no PostgreSQL. Poller desativado no teste para nao exigir Elasticsearch.
- OpenAPI define titulo, versao, seguranca bearer JWT e descreve operacoes e
  respostas de erro. Cadastro e login continuam publicos no contrato.
- Micrometer publica quantidade de eventos pendentes, sucessos e falhas de
  indexacao, e falhas de escrita de auditoria. Tags usam apenas valores de
  cardinalidade limitada, sem IDs, email ou dados de requisicao.
- Um script sobe o Compose em projeto isolado, espera healthchecks, consulta
  health, OpenAPI e metricas, percorre cadastro/login/criacao, e remove apenas
  recursos que criou. A CI executa esse
  script em job de smoke; o job atual de verificacao continua.
- README explica execucao local, exemplos e metricas. `index.md` acompanha
  as novas pastas.

## Limites

O teste principal nao valida Elasticsearch real; a verificacao Compose cobre
sua inicializacao. Auditoria continua best effort conforme Marco 10. A rota
Prometheus continua protegida por autenticacao; acesso operacional deve ser
configurado deliberadamente em implantacao real.
