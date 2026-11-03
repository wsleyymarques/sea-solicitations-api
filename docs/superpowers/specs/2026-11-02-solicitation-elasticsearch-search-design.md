# Marco 8: Indexacao e Busca de Solicitacoes

## Intencao

Permitir pesquisa paginada das solicitacoes por analistas usando Elasticsearch,
sem confiar em filtros de UF ou identidade enviados pelo cliente. PostgreSQL
continua sendo a fonte de verdade.

## Contratos

- `GET /analyst/solicitations/search` aceita `text`, `status`, `serviceType`,
  `priority`, `state`, `dateFrom`, `dateTo`, `page` (padrao 0),
  `size` (padrao 20, maximo 100), `sort` (`SUBMITTED_AT`, `CREATED_AT`,
  `PREFERRED_DATE`) e `direction` (`ASC`, `DESC`; padrao `DESC`).
- `submittedFrom`, `submittedTo` e `sortBy` continuam aceitos como aliases de
  compatibilidade; os parametros canonicos prevalecem quando ambos aparecem.
- As datas sao dias UTC inclusivos do instante de submissao. `from > to` e
  paginacao alem da janela de 10.000 documentos do Elasticsearch recebem 400.
- A resposta contem `content`, `page`, `size`, `totalElements`, `totalPages`.
  Cada item traz identificacao, status, tipo, titulo, prioridade, UF, data de
  criacao/submissao, data preferida e analista atribuido.
- Solicitacoes `DRAFT` nao aparecem. `SUBMITTED` aparece para analistas cuja
  cobertura inclui a UF. `IN_REVIEW`, `APPROVED` e `REJECTED` aparecem somente
  ao analista atribuido, sempre dentro de sua cobertura. Um filtro opcional
  `state` apenas restringe esse conjunto. Cobertura vazia resulta em pagina
  vazia sem consultar Elasticsearch.

## Indexacao

Documento `solicitations-v1` com texto analisado em portugues, facetas
`keyword` e datas `date`. A indexacao usa o ID da solicitacao como ID do
documento, permitindo sobrescrever a versao anterior. Um decorador da porta
`SolicitationRepository` indexa apos cada `save`, cobrindo criar, editar
endereco e confirmacao, submeter, iniciar revisao e decidir. Iniciar revisao e
necessario para remover imediatamente
uma solicitacao da lista de outros analistas.

Cada hit e conferido no PostgreSQL antes da resposta e os campos devolvidos
vem do agregado atual. Isso impede vazamento por documento atrasado apos a
atribuicao. Enquanto houver atraso, uma pagina pode conter menos itens que
`size`, e `totalElements` reflete a contagem do indice.

O index e criado com mapping explicito na primeira operacao. Falha de
Elasticsearch aparece como `503 SEARCH_UNAVAILABLE`. Nesta etapa as escritas
sao sincronas: uma falha entre o commit relacional e a indexacao pode deixar
o documento ausente ou desatualizado. A outbox do Marco 9 eliminara essa lacuna.
Dados gravados antes da instalacao deste marco precisam de reindexacao.

## Abordagens

1. **Porta de indexacao/busca e adapter Spring Data Elasticsearch — escolhida.**
   Mantem consulta e mapping isolados e permite trocar o envio direto pela
   outbox no Marco 9.
2. Consulta no PostgreSQL. Nao entregaria o motor de busca exigido.
3. Chamada HTTP direta ao Elasticsearch nos controllers. Misturaria regra de
   acesso e infraestrutura com transporte HTTP.

## Verificacao

Testes de servico verificam indexacao em cada transicao e busca com cobertura
do JWT. Testes do adapter inspecionam o documento, a query nativa, filtros,
ordenacao e mapping. Testes de controller cobrem contrato, RBAC e entrada
invalida. A suite Maven completa deve passar; teste real depende de Docker.
