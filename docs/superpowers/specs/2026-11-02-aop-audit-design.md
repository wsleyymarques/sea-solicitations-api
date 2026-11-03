# Marco 10 — Auditoria por AOP

## Intencao

Registrar operacoes de escrita relevantes com ator, acao, recurso, resultado,
request id, metadados seguros e instante, sem alterar as regras dos casos de
uso. Os dados vao para `audit_logs`, ja criado pela migration V1.

## Abordagens

1. **Annotation nos casos de uso e aspect (escolhida):** declaracao pequena
   da acao; o aspect envolve o metodo e usa extracao tipada e restrita de
   contexto. Centraliza sucesso, falha e persistencia.
2. Chamadas manuais em cada service: explicitas, mas repetem tratamento de
   sucesso/falha e permitem esquecer uma transicao.
3. Interceptacao HTTP: cobre requests, mas perde resultado transacional e
   chamadas fora de controllers.

## Contrato

- Anotar cadastro de cliente e usuario interno, criacao e demais transicoes
  de solicitacao, inicio de analise e decisao. Criacao de solicitacao grava
  tambem `BASIC_DATA_SAVED`, pois essa etapa e concluida no mesmo caso de uso.
  A decisao distingue aprovacao e rejeicao. Criacao de analista produz tambem
  `ANALYST_STATES_ASSIGNED`.
- O ator vem do JWT autenticado quando presente; cadastro publico usa o ID
  criado no sucesso. Fora de HTTP, comandos com `clientId`/`analystId` podem
  identificar o ator.
- O recurso de criacoes vem do resultado; nas demais operacoes, do comando.
  Falhas sem recurso criado deixam `resource_id` vazio.
- `X-Request-Id` valido (ate 100 caracteres, ASCII seguro) e preservado;
  caso ausente ou invalido, gerar UUID. O mesmo ID e usado em eventos da
  mesma operacao.
- Metadados sao montados por lista permitida: papel, UFs, tipo de servico,
  prioridade, desfecho e classe da excecao. Nunca serializar comando, resposta,
  senha, token, comentario, endereco ou mensagem da excecao.
- O aspect fica externo a transacao de negocio e grava cada log em transacao
  propria (`REQUIRES_NEW`). Assim falhas de negocio tambem sao registradas.
  Falha de auditoria e registrada no log da aplicacao sem substituir a resposta
  ou a excecao original.

## Limites e verificacao

O log de sucesso e gravado apos o retorno da transacao de negocio; por isso
uma falha isolada da escrita de auditoria pode deixar uma lacuna, visivel nos
logs da aplicacao. O Marco 10 nao inclui consulta HTTP a auditoria nem
retencao/expurgo. Testes cobrem classificacao, redacao de dados sensiveis,
falhas, request id, transacao separada e aplicacao real do aspect.
