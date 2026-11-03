# Revisão final da API e dos clientes de teste

## Escopo e resultado

Revisão do estado de trabalho dos marcos 5 a 11, dos contratos HTTP usados pelo
novo painel Vite e da coleção Postman. Nenhum defeito bloqueante foi encontrado
nos fluxos exercitados. A cobertura automatizada foi ampliada para percorrer
cadastro, login, submissão, aprovação e rejeição com PostgreSQL real. A coleção
Postman percorre o mesmo fluxo com Elasticsearch real em ambiente Compose.

## Verificações executadas

| Verificação | Resultado |
| --- | --- |
| `./mvnw -q verify` | 223 testes Surefire, 0 falhas, 0 erros, 9 ignorados |
| `./mvnw -q -Pintegration verify` | 3 testes Failsafe, 0 falhas; ciclo de aprovação/rejeição e bloqueios de acesso com PostgreSQL real |
| `bash scripts/smoke-compose.sh` | API, PostgreSQL e Elasticsearch saudáveis; cadastro, login, OpenAPI, métricas, criação e indexação real via outbox |
| Scripts de persistência e bootstrap | Consulta do analista, usuários internos, outbox, auditoria e primeiro administrador passaram com JDK 21 |
| `npm run build` em `frontend/` | TypeScript e build Vite concluídos |
| Painel no navegador | API conectada; cadastro, login e criação retornaram 201/200/201; erro sem sessão exibido |
| Newman com Compose descartável | 20 requisições e 24 asserções passaram, da saúde da API à aprovação e rejeição |
| `docker compose --env-file .env.example config --quiet` e `git diff --check` | Sem erros |

## Pontos observados

- A autorização de cliente, administrador e analista continua no backend.
  O painel guarda tokens apenas na memória da aba e valida o papel do token
  para evitar indicar uma sessão com perfil errado.
- A gravação da solicitação e do evento de outbox ocorre na mesma transação
  PostgreSQL. A indexação é assíncrona; uma busca imediata pode não refletir
  o último estado.
- A busca usa Elasticsearch para candidatos e confere novamente no banco
  a cobertura e a atribuição do analista. O total da paginação vem do índice
  e pode diferir da quantidade visível depois dessa conferência.
- A auditoria usa uma transação separada e não altera o resultado da operação
  de negócio se a gravação do log falhar. A falha é registrada em log e métrica.
- O CI cobre o ciclo até as duas decisões no teste HTTP com PostgreSQL e a
  indexação real no smoke Compose. A coleção Postman também exercita a busca
  com Elasticsearch real; sua execução continua manual.
- ViaCEP e Elasticsearch são dependências externas em execução real. Os testes
  de integração simulam o CEP para manter o resultado reproduzível; o Postman
  confirmou a consulta real ao ViaCEP no ambiente descartável desta revisão.
- O repositório contém alterações pendentes dos marcos anteriores. Esta
  revisão não criou commits nem removeu alterações preexistentes.

## Perguntas de entrevista

**Por que usar outbox em vez de gravar diretamente no Elasticsearch?**
Porque a solicitação e o pedido de indexação ficam na mesma transação do
PostgreSQL. Se o Elasticsearch estiver indisponível, o evento permanece para
nova tentativa, sem perder a atualização.

**Como dois analistas são impedidos de assumir a mesma solicitação?**
A transição exige `SUBMITTED` e a persistência usa controle de versão.
Uma gravação concorrente falha com `409`, preservando a atribuição vencedora.

**Por que a auditoria não faz parte da transação de negócio?**
Ela registra também operações que falham e não deve mudar o resultado da
operação auditada. `REQUIRES_NEW` isola a gravação; falhas de auditoria são
observáveis por log e contador.
