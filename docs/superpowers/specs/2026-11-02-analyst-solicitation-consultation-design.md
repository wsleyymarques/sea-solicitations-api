# Desenho da Consulta de Solicitacoes pelo Analista

## Intencao

Permitir que um analista encontre, de forma paginada, as solicitacoes que pode
iniciar ou continuar analisando. A consulta completa o Marco 6 sem antecipar a
busca avancada do Marco 8.

## Contrato HTTP

- Metodo e caminho: `GET /analyst/solicitations`.
- Parametros: `page` (padrao `0`, minimo `0`) e `size` (padrao `20`, entre `1`
  e `100`).
- Ordenacao fixa: `submittedAt DESC`, com `id DESC` como desempate estavel.
- Resposta: `content`, `page`, `size`, `totalElements` e `totalPages`.
- Cada item contem `id`, `status`, `serviceType`, `title`, `priority`, `state`,
  `submittedAt`, `assignedAnalystId` e `reviewStartedAt`.

## Visibilidade e autorizacao

O backend usa exclusivamente `sub` e `coveredStates` do JWT. Uma solicitacao
fica visivel quando sua UF pertence a cobertura e uma destas condicoes e
verdadeira:

1. esta em `SUBMITTED`, portanto disponivel para inicio da analise; ou
2. esta em `IN_REVIEW` e atribuida ao analista autenticado.

Solicitacoes `DRAFT`, terminais ou em analise por outro analista nao aparecem.
Claim de cobertura ausente ou malformada continua produzindo
`403 SOLICITATION_REVIEW_ACCESS_DENIED`. Cobertura vazia produz uma pagina
vazia, sem ampliar o acesso.

## Arquitetura

O controller traduz JWT e paginacao para um caso de uso de entrada. O service
de aplicacao delega a uma operacao de consulta adicionada a
`SolicitationRepository` e transforma os agregados em resultados imutaveis.
O adapter JPA aplica visibilidade e ordenacao na propria consulta para que
dados fora da cobertura nunca sejam carregados para filtragem em memoria.

## Abordagens consideradas

1. **Consulta basica no PostgreSQL — escolhida.** Mantem o banco relacional
   como fonte de verdade e entrega o Marco 6 sem depender da indexacao ainda
   inexistente.
2. Consulta pelo Elasticsearch. Ofereceria filtros ricos, mas anteciparia o
   Marco 8 e criaria dependencia de sincronizacao antes da outbox do Marco 9.
3. Carregar solicitacoes e filtrar em memoria. Foi descartada porque quebra a
   paginacao, amplia a exposicao interna de dados e escala mal.

## Nao objetivos

- Filtros por texto, status, tipo, prioridade, UF ou periodo.
- Endpoint de detalhe da solicitacao.
- Indexacao Elasticsearch, outbox, auditoria ou decisao da analise.

## Verificacao

Testes cobrem a regra de visibilidade no service, a traducao e ordenacao do
adapter JPA, o contrato HTTP, claims invalidas, paginacao invalida e RBAC. Um
teste opt-in executa a JPQL, a contagem e duas paginas contra PostgreSQL real
em container descartavel. A suite Maven completa deve permanecer verde.
