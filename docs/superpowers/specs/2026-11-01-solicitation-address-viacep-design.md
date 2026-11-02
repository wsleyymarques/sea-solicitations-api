# Desenho da Etapa de Endereco com ViaCEP

## Intencao

Permitir que o cliente proprietario complete ou edite o endereco de uma solicitacao `DRAFT`. O cliente informa CEP, numero e complemento; o backend consulta o ViaCEP, completa logradouro, bairro, cidade e UF, persiste o endereco e avanca a solicitacao para `CONFIRMATION`.

## Contrato HTTP

- Metodo e caminho: `PUT /solicitations/{id}/address`.
- Autorizacao: somente `CLIENT`; o proprietario vem do `sub` do JWT.
- Entrada: `postalCode`, `number` e `complement` opcional.
- Saida: identificador, `DRAFT`, proxima etapa `CONFIRMATION` e o endereco persistido.
- O corpo nunca aceita `clientId`, rua, bairro, cidade ou UF como fontes de verdade.

## Abordagens consideradas

### Consulta no controller

O controller poderia chamar o ViaCEP e entregar um `Address` pronto ao caso de uso. Foi rejeitada porque mistura HTTP de entrada com HTTP de saida, dificulta testes e espalha o contrato externo.

### Porta de consulta e adapter sincrono — escolhida

O caso de uso depende de uma porta `PostalCodeLookup`; `ViaCepPostalCodeAdapter` implementa essa interface com `RestClient`. O adapter traduz JSON, erros e indisponibilidade para conceitos da aplicacao. Esta abordagem preserva a arquitetura existente e permite substituir o ViaCEP por um fake nos testes.

### Enriquecimento assincrono

O endereco poderia ser resolvido por evento depois da requisicao. Foi rejeitada porque a etapa precisa de resposta imediata, adicionaria estados intermediarios e nao traz beneficio proporcional ao desafio.

## Fluxo

1. O controller valida formato do CEP, numero e complemento e extrai o cliente do JWT.
2. O caso de uso carrega a solicitacao.
3. O caso de uso verifica ownership, `DRAFT` e dados basicos preenchidos antes da chamada externa.
4. `PostalCodeLookup` consulta o CEP normalizado.
5. O caso de uso combina os dados retornados com numero e complemento.
6. O agregado executa `completeAddress`, mantendo suas invariantes.
7. O repositorio salva o agregado; `@Version` protege contra atualizacao concorrente.

A chamada externa nao ganha retry automatico. Timeouts curtos limitam a espera e evitam multiplicar requisicoes quando o ViaCEP estiver instavel.

## Interfaces e adapters

- `CompleteSolicitationAddressUseCase`: porta de entrada do fluxo.
- `SolicitationRepository.findById(UUID)`: carrega o agregado para autorizacao e edicao.
- `PostalCodeLookup.find(String)`: pequena interface independente de fornecedor.
- `ViaCepPostalCodeAdapter`: conhece `/ws/{cep}/json/` e o JSON em portugues.
- `ViaCepProperties`: URL base, timeout de conexao e timeout de leitura.

Os DTOs do ViaCEP permanecem privados ao adapter. A aplicacao recebe apenas `PostalCodeData`, com nomes e tipos do nosso dominio.

## Transacao e concorrencia

O fluxo nao mantem uma transacao de banco aberta durante a chamada de rede. A leitura e a gravacao usam as transacoes do adapter JPA, e a versao otimista da entidade detecta alteracoes concorrentes. O caso de uso valida ownership e estado antes da consulta; o `save` faz a gravacao curta ao final.

## Erros

- `400 VALIDATION_ERROR`: CEP, numero ou complemento invalidos na entrada.
- `403 SOLICITATION_ACCESS_DENIED`: solicitacao pertence a outro cliente.
- `404 SOLICITATION_NOT_FOUND`: identificador inexistente.
- `409 INVALID_SOLICITATION_STATE`: solicitacao nao editavel ou sem dados basicos.
- `409 SOLICITATION_CONCURRENT_UPDATE`: outra requisicao alterou o mesmo rascunho.
- `422 POSTAL_CODE_NOT_FOUND`: CEP valido, mas inexistente ou sem endereco completo.
- `503 POSTAL_CODE_SERVICE_UNAVAILABLE`: timeout, falha de conexao ou erro HTTP do provedor.

Falhas de validacao, autorizacao ou ViaCEP ocorrem antes de `completeAddress` e
`save`. Em um conflito de concorrencia, `save` detecta a versao obsoleta e a
transacao nao grava a alteracao concorrente.

## Testes

- Service: sucesso, ownership, inexistencia e estado invalido sem chamada externa indevida.
- Adapter: resposta completa, `erro=true`, resposta incompleta e falha HTTP.
- Web: JWT vira `clientId`, resposta `200`, validacao `400` e erros `422`/`503` padronizados.
- Suite completa: garante regressao do cadastro, autenticacao e criacao da solicitacao.

## Fora do escopo

- cache de CEP;
- retry e circuit breaker;
- preenchimento manual de logradouro;
- confirmacao, submissao, Elasticsearch e outbox;
- teste de integracao com o ViaCEP real.
