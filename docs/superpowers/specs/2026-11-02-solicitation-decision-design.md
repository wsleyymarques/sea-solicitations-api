# Desenho da Decisao da Solicitacao

## Intencao

Concluir o Marco 7: o analista que iniciou a analise aprova ou rejeita a
solicitacao, registrando um comentario e o instante da decisao. O resultado e
terminal, persistido e consultavel no proprio registro da solicitacao.

## Contrato HTTP

`POST /analyst/solicitations/{id}/decision` recebe `outcome` (`APPROVED` ou
`REJECTED`) e `comment` (10 a 1000 caracteres apos remover espacos externos).
Responde `200 OK` com `id`, `status`, `analystId`, `comment` normalizado e
`decidedAt`. O analista e obtido do `sub` do JWT; a cobertura vem de
`coveredStates`. Nenhum dos dois pode ser escolhido no corpo.

Corpo ausente ou invalido responde `400`. Solicitacao inexistente responde
`404`. UF fora da cobertura ou analista diferente do atribuido responde `403`.
Status diferente de `IN_REVIEW`, inclusive uma segunda decisao, responde `409`.
Conflito de versao na persistencia tambem responde `409`.

## Regras e limites

- A checagem de cobertura antecede a atribuicao e usa a UF persistida.
- A decisao e aplicada apenas ao agregado em `IN_REVIEW` atribuido ao ator.
- A validacao do comentario tambem ocorre no dominio para proteger chamadas
  que nao passam pelo HTTP.
- `@Version` protege duas decisoes concorrentes; a primeira gravacao vence.
- Os campos `decision_analyst_id`, `decision_comment` e `decided_at` ja existem
  no schema e fornecem a trilha da decisao. O log generico por AOP pertence ao
  Marco 10.

## Abordagens

1. **Endpoint unico com resultado explicito — escolhida.** Uma rota e um caso
   de uso compartilham a mesma autorizacao e regra terminal.
2. Rotas separadas `/approve` e `/reject`. Sao simples para o cliente, mas
   duplicariam o contrato e a verificacao de acesso.

## Verificacao

Testes de caso de uso cobrem os dois resultados, autorizacao, estado,
comentario e concorrencia. Testes HTTP cobrem identidade do JWT, validacao,
RBAC e erros. A suite Maven completa verifica regressao.
