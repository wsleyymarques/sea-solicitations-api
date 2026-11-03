# Marco 9: Outbox Transacional

## Intencao

Garantir que uma alteracao de solicitacao confirmada no PostgreSQL produza um
pedido duravel de indexacao, mesmo quando o Elasticsearch esta indisponivel.
PostgreSQL permanece como fonte de verdade.

## Escrita

O decorador de `SolicitationRepository` grava a solicitacao e um
`SOLICITATION_INDEX_REQUESTED` na mesma transacao. O evento guarda o UUID do
agregado; o worker le o estado atual da solicitacao na hora de indexar. Toda
transicao que usa `save` continua coberta. Falha do PostgreSQL em qualquer
das duas gravacoes reverte ambas; Elasticsearch nao participa da requisicao.

## Processamento

Um poller roda periodicamente e chama um processador transacional. Uma query
PostgreSQL seleciona os eventos pendentes prontos por `next_attempt_at`,
ordenados por `occurred_at, id`, com `FOR UPDATE SKIP LOCKED`. Eventos mais
novos do mesmo agregado aguardam o mais antigo. Isso permite multiplas
instancias sem processar o mesmo evento simultaneamente nem inverter a ordem
de um agregado.

Ao indexar, o worker marca `processed_at`. Se o Elasticsearch falhar, o worker
incrementa `attempts`, guarda um erro curto e agenda retry exponencial de 5
segundos ate 5 minutos. Eventos continuam elegiveis indefinidamente. Se o
processo cair apos a escrita no Elasticsearch e antes do commit, o evento
sera reprocessado; o ID estavel do documento torna a operacao idempotente.
Evento cujo agregado nao existe e marcado como processado, pois nao ha
solicitacao a indexar. Falhas de banco fazem a transacao do lote reverter e
serao tentadas no proximo ciclo.

## Contrato operacional

O indice e eventualmente consistente. Uma escrita de negocio retorna sucesso
quando PostgreSQL e outbox confirmam, sem esperar Elasticsearch. Busca pode
atrasar enquanto houver eventos pendentes. O processador usa lotes de ate 20
eventos e um intervalo configuravel (`outbox.poll-interval-ms`, padrao 5000).

## Abordagens

1. **Polling com lock PostgreSQL — escolhida.** Usa a tabela e o schema
   existentes, com uma migration para `next_attempt_at`, e evita uma fila
   externa neste desafio.
2. Publicacao apos commit com evento Spring. Nao sobrevive a queda do processo
   entre o commit e a publicacao.
3. Indexacao sincrona com retry na requisicao. Mantem a escrita de negocio
   dependente do Elasticsearch e aumenta a latencia.

## Limites

O processamento e pelo menos uma vez, portanto o adapter de indexacao deve
permanecer idempotente. Um lote mantem locks de banco durante chamadas ao
Elasticsearch; o tamanho pequeno limita o tempo de retencao. Dados anteriores
ao Marco 8 ainda precisam de reindexacao especifica.

## Verificacao

Testes de dominio cobrem backoff e transicoes. Testes de servico cobrem escrita
da outbox sem chamada sincrona ao Elasticsearch, processamento, retry,
ordenacao e eventos repetidos. Testes do adapter verificam a query de claim.
Um teste opcional com PostgreSQL descartavel executa Flyway V1/V2 e verifica
escrita atomica, rollback, ordem, retry e `SKIP LOCKED` entre transacoes.
O fluxo completo com Elasticsearch real depende de infraestrutura local.
