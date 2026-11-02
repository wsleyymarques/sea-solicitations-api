# Desenho da Submissao da Solicitacao

## Intencao

Permitir que o cliente proprietario envie uma solicitacao completa para
analise. A submissao encerra a edicao do rascunho, registra o instante da
transicao e altera o status de `DRAFT` para `SUBMITTED`.

## Contrato HTTP

- Metodo e caminho: `POST /solicitations/{id}/submit`.
- Autorizacao: somente `CLIENT`; o proprietario vem do `sub` do JWT.
- Entrada: sem corpo.
- Saida `200 OK`: `id`, status `SUBMITTED`, etapa `COMPLETED` e
  `submittedAt`.
- O instante e a data atual derivam do mesmo `Clock` compartilhado.

## Regras

- A solicitacao precisa existir e pertencer ao cliente autenticado.
- Somente uma solicitacao `DRAFT` pode ser submetida.
- Dados basicos, endereco e confirmacao precisam estar completos.
- A confirmacao e revalidada na submissao; a data preferida nao pode ter se
  tornado passada desde o preenchimento.
- Uma submissao valida registra `submittedAt`, muda apenas o status para
  `SUBMITTED` e preserva `currentStep` como `COMPLETED`.
- Depois da submissao, uma segunda tentativa e qualquer edicao retornam
  conflito.
- Nenhuma validacao pode deixar mutacao parcial no agregado ou disparar
  persistencia.

## Erros

- `404 SOLICITATION_NOT_FOUND`: identificador inexistente.
- `403 SOLICITATION_ACCESS_DENIED`: solicitacao de outro cliente, sem expor
  seu progresso.
- `409 INCOMPLETE_SOLICITATION`: lista estruturada das etapas ausentes nos
  campos `basicData`, `address` e/ou `confirmation`.
- `409 INVALID_SOLICITATION_STATE`: status diferente de `DRAFT` ou
  confirmacao cuja data preferida venceu.
- `409 SOLICITATION_CONCURRENT_UPDATE`: outra requisicao alterou a versao.

## Abordagens consideradas

### Reutilizar apenas `INVALID_SOLICITATION_STATE`

Seria a menor mudanca, mas manteria a mensagem generica atual e nao atenderia
ao requisito de indicar com clareza o que falta.

### Tratar incompletude como `422`

Representaria uma entidade semanticamente invalida, mas a requisicao nao
possui corpo incorreto; o conflito esta no estado atual do recurso.

### Erro dedicado `409 INCOMPLETE_SOLICITATION` — escolhida

Mantem a semantica de conflito de estado e fornece violacoes estruturadas que
um cliente pode apresentar por etapa. A invariante permanece no dominio por
meio de uma exception que carrega as etapas ausentes; o adapter HTTP apenas
traduz os nomes para o contrato JSON.

## Fluxo e persistencia

1. O controller extrai o cliente do JWT e cria o comando.
2. O service carrega a solicitacao e valida ownership e `DRAFT`.
3. O agregado verifica completude, revalida a confirmacao e executa a
   transicao.
4. O repositorio salva o agregado com controle otimista por `@Version`.
5. O resultado usa o identificador devolvido pela persistencia e os valores
   efetivamente gravados no agregado.

O schema existente ja possui `submitted_at`, os status necessarios e a
constraint que exige etapa completa e timestamp para solicitacoes submetidas.
Nao sera criada migration.

## Fora do escopo

- atribuicao ou inicio da analise;
- Elasticsearch e outbox;
- auditoria AOP;
- idempotencia que transforme uma segunda submissao em sucesso;
- alteracao do schema.
