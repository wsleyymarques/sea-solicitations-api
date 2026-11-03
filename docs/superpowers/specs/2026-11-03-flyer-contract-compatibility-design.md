# Compatibilidade do Contrato do Flyer e Robustez Tecnica

## Intencao

Alinhar a API ao contrato publicado no flyer do desafio sem quebrar os
clientes existentes, e corrigir dois riscos tecnicos identificados na revisao:
o tratamento tardio de conflitos de lock otimista e o acesso amplo as
metricas Prometheus.

## Objetivos

- Implementar `GET /analyst/solicitations/{id}` com detalhe completo e
  autorizacao por coverage de UF.
- Aceitar `POST /analyst/solicitations/{id}/decide` como rota oficial do
  flyer, preservando `/decision` como alias compativel.
- Aceitar `dateFrom`, `dateTo` e `sort` na busca, preservando os parametros
  atuais como aliases.
- Garantir que conflitos de versao JPA respondam
  `409 SOLICITATION_CONCURRENT_UPDATE`, inclusive quando detectados no flush.
- Restringir `/actuator/prometheus` a usuarios `ADMIN`.
- Sincronizar testes, README, frontend e colecao Postman com o contrato do
  flyer.

## Nao objetivos

- Criar endpoints de consulta ou listagem de rascunhos para CLIENT.
- Alterar as regras de estado, ownership, coverage ou indexacao por outbox.
- Eliminar a consistencia eventual entre PostgreSQL e Elasticsearch.
- Criar revogacao imediata de JWT ou administracao de usuarios ativos.

## Contratos HTTP

### Detalhe para o analista

`GET /analyst/solicitations/{id}` exige papel `ANALYST`. O backend obtem o
analista e as UFs exclusivamente do JWT. A solicitacao responde `200` apenas
quando seu endereco pertence ao coverage autenticado e ela esta visivel ao
analista:

- `SUBMITTED`, ainda sem atribuicao; ou
- `IN_REVIEW`, `APPROVED` ou `REJECTED`, quando atribuida ao proprio analista.

Solicitacao inexistente responde `404`; UF fora da cobertura ou solicitacao
atribuida a outro analista responde `403`. `DRAFT` tambem responde `403`, pois
nao pertence ao fluxo interno antes da submissao.

A resposta inclui `id`, `clientId`, dados basicos, endereco, confirmacao,
`status`, `currentStep`, `assignedAnalystId`, `submittedAt`,
`reviewStartedAt`, decisao quando existente, `createdAt` e `updatedAt`. Nao
inclui entidades JPA diretamente.

### Decisao

`POST /analyst/solicitations/{id}/decide` e a rota canonica. A rota existente
`POST /analyst/solicitations/{id}/decision` permanece como alias temporario e
devolve o mesmo contrato, autorizacao e codigos HTTP.

### Busca

Os nomes canonicos do flyer sao:

- `dateFrom` e `dateTo`, dias UTC inclusivos de `submittedAt`;
- `sort`, com os valores existentes `SUBMITTED_AT`, `CREATED_AT` e
  `PREFERRED_DATE`;
- `direction`, preservado para expressar `ASC` ou `DESC`.

`submittedFrom`, `submittedTo` e `sortBy` continuam aceitos como aliases. Se o
mesmo valor for informado pelo nome canonico e pelo alias, o valor canonico
prevalece. Validacao de intervalo, paginacao e janela maxima permanece igual.

## Concorrencia e erros

O adapter JPA deve provocar o flush da solicitacao dentro do trecho que
traduz `ObjectOptimisticLockingFailureException`. Assim, um update concorrente
nao escapa no commit como `500`; ele se torna
`SolicitationConcurrentUpdateException` e usa o JSON padronizado com status
`409`.

O evento de outbox continua na mesma transacao PostgreSQL. Se a gravacao do
evento falhar, solicitacao e evento sofrem rollback juntos.

## Seguranca das metricas

`GET /actuator/health` continua publico. OpenAPI/Swagger continuam publicos.
`/actuator/prometheus` passa a exigir `ROLE_ADMIN`; CLIENT, ANALYST e usuario
anonimo recebem `403`, `403` e `401`, respectivamente. O smoke test usa token
de administrador para consultar metricas.

## Abordagens consideradas

1. **Aliases retrocompativeis e endpoint detalhado dedicado — escolhida.**
   Entrega exatamente o flyer e preserva frontend, Postman e consumidores
   existentes durante a transicao.
2. Renomear e remover imediatamente contratos antigos. E mais simples, mas
   quebra os clientes atuais sem necessidade.
3. Manter apenas os contratos atuais e documentar a diferenca. Nao atende um
   avaliador automatizado que siga literalmente o flyer.

## Verificacao

- Testes de controller cobrem a rota individual, coverage, atribuicao, papel,
  `/decide` e o alias `/decision`.
- Testes da busca cobrem nomes canonicos, aliases e precedencia canonica.
- Teste de persistencia com PostgreSQL real reproduz duas atualizacoes
  concorrentes e confirma a traducao para `409` no nivel HTTP ou para
  `SolicitationConcurrentUpdateException` no adapter.
- Testes de seguranca cobrem Prometheus para anonimo, CLIENT, ANALYST e ADMIN.
- O teste HTTP integrado usa os caminhos e parametros canonicos do flyer.
- Frontend, gerador/colecao Postman e README usam os nomes canonicos; os aliases
  permanecem testados para compatibilidade.
- `./mvnw verify`, `./mvnw -Pintegration verify`, build do frontend e validacao
  da colecao gerada devem permanecer verdes.
