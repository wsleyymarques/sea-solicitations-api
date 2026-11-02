# Desenho da Etapa de Confirmacao

## Intencao

Permitir que o cliente proprietario complete ou edite a terceira etapa de uma
solicitacao `DRAFT`. O backend salva prioridade, data preferida, valor estimado
opcional e aceite dos termos, mantendo o rascunho pronto para a futura
submissao.

## Contrato HTTP

- Metodo e caminho: `PUT /solicitations/{id}/confirmation`.
- Autorizacao: somente `CLIENT`; o proprietario vem do `sub` do JWT.
- Entrada: `priority`, `preferredDate`, `estimatedValue` opcional e
  `termsAccepted`.
- Saida: identificador, status `DRAFT`, etapa `COMPLETED` e a confirmacao
  persistida.
- A data atual e obtida por um `Clock` compartilhado entre o caso de uso e o
  Bean Validation, permitindo teste deterministico e evitando divergencia de
  fuso horario entre as camadas.

## Regras

- A solicitacao precisa existir, pertencer ao cliente autenticado e estar
  `DRAFT`.
- Dados basicos e endereco precisam estar completos.
- `preferredDate` pode ser hoje ou uma data futura, mas nunca passada.
- `estimatedValue`, quando informado, deve ser maior ou igual a zero e caber
  em `NUMERIC(19,2)`.
- Prioridade `HIGH` exige `estimatedValue` maior ou igual a `100.00`.
- Prioridades `LOW` e `MEDIUM` permitem valor ausente.
- `termsAccepted` deve ser `true`.
- A confirmacao pode ser editada enquanto a solicitacao continuar `DRAFT`.

## Abordagens consideradas

### Validar somente no DTO

Rejeitaria entradas HTTP cedo, mas permitiria violar regras ao chamar o caso
de uso por outro adapter. Foi descartada como unica protecao.

### Validar tudo no service

Concentraria o fluxo, mas duplicaria regras que ja pertencem aos objetos
`Confirmation` e `Solicitation`. Foi descartada por reduzir a profundidade do
dominio.

### Validacao em camadas — escolhida

O DTO protege formato, obrigatoriedade, sinal, escala e data na borda. O caso
de uso autoriza e fornece a data atual; o dominio protege ordem das etapas,
estado, aceite, data e a regra cruzada de prioridade alta. Assim, o controller
permanece fino e o modulo de dominio oferece uma interface pequena com as
invariantes escondidas na implementacao.

## Fluxo e persistencia

1. O controller valida o JSON e extrai o cliente do JWT.
2. O caso de uso carrega a solicitacao e valida ownership, `DRAFT` e etapas
   anteriores.
3. O caso de uso cria `Confirmation` e chama `completeConfirmation` com
   `LocalDate.now(clock)`.
4. O agregado valida e avanca `currentStep` para `COMPLETED`.
5. O repositorio salva o agregado; `@Version` detecta escrita concorrente.

Nao existe chamada externa neste fluxo. O schema atual ja possui as colunas e
constraints da confirmacao, portanto nenhuma migration sera criada.

## Erros

- `400 VALIDATION_ERROR`: campos ausentes, data passada, valor negativo,
  excesso de digitos ou termos nao aceitos na borda HTTP.
- `400 INVALID_REQUEST`: regra cruzada de prioridade alta violada no dominio.
- `403 SOLICITATION_ACCESS_DENIED`: solicitacao de outro cliente.
- `404 SOLICITATION_NOT_FOUND`: identificador inexistente.
- `409 INVALID_SOLICITATION_STATE`: solicitacao nao editavel ou etapas
  anteriores incompletas.
- `409 SOLICITATION_CONCURRENT_UPDATE`: versao alterada por outra requisicao.

## Fora do escopo

- submissao da solicitacao;
- alteracao do schema;
- auditoria e outbox;
- calculo automatico de valor;
- novos perfis ou permissoes.
