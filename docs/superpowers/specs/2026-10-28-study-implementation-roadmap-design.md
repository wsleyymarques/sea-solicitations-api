# Desenho do Roteiro de Estudo e Implementacao

## Intencao

Este documento define como o desafio backend sera estudado e implementado de forma gradual. O objetivo nao e apenas entregar uma API funcional, mas permitir que o autor compreenda as decisoes, consiga justificar alternativas e responda perguntas tecnicas durante a avaliacao da vaga.

O roteiro nao possui datas fixas. O progresso sera medido por marcos funcionais e de aprendizado.

## Objetivos

- Entregar todos os requisitos obrigatorios do desafio sem criar dominios extras.
- Trabalhar em incrementos pequenos, executaveis e testaveis.
- Explicar o papel de cada camada, porta, adapter, entidade e configuracao.
- Registrar decisoes e trade-offs importantes para a entrevista.
- Usar testes para orientar e comprovar cada comportamento relevante.
- Manter o projeto executavel com Java 21, Docker Compose, PostgreSQL e Elasticsearch.
- Implementar os diferenciais somente depois do fluxo obrigatorio correspondente.

## Nao objetivos

- Transformar o sistema em microsservicos.
- Adicionar Kafka, RabbitMQ, Redis, MongoDB, pagamentos, anexos ou notificacoes.
- Implementar tecnologias citadas na vaga sem relacao com o desafio.
- Produzir codigo antecipado para funcionalidades que ainda nao possuem caso de uso.
- Priorizar velocidade de entrega em detrimento da compreensao do autor.

## Antiobjetivos

O roteiro sera considerado mal sucedido se o sistema funcionar, mas o autor nao conseguir explicar as principais decisoes; se houver muitas abstracoes sem uso; se os testes nao cobrirem as regras criticas; ou se os diferenciais atrasarem o fluxo principal.

## Restricoes

- Java 21 e Spring Boot.
- Backend only.
- PostgreSQL como fonte de verdade transacional.
- Elasticsearch para busca com filtros e paginacao.
- Autenticacao JWT e autorizacao por perfil, proprietario e cobertura de UF.
- Docker Compose com aplicacao, PostgreSQL e Elasticsearch.
- Bean Validation e erros JSON padronizados.
- Testes unitarios e pelo menos um teste de integracao real.
- Auditoria simples por AOP.
- Os caminhos obrigatorios do flyer devem ser preservados.
- O documento completo do desafio, quando disponibilizado, prevalece sobre inferencias feitas a partir do flyer.

## Estado inicial

O projeto ja possui:

- estrutura de monolito modular com arquitetura hexagonal pragmatica;
- dominio de usuarios, solicitacoes, auditoria e outbox;
- migration inicial do PostgreSQL;
- Dockerfile e Docker Compose;
- cadastro de cliente e autenticacao JWT completos;
- testes unitarios e testes web do fluxo de autenticacao.

Ainda faltam os casos de uso e adapters das solicitacoes, administracao, autorizacao contextual, integracao ViaCEP, Elasticsearch funcional, processamento da outbox, auditoria AOP e teste de integracao com infraestrutura real.

## Abordagens consideradas

### Por camadas tecnicas

Implementar toda a persistencia, depois toda a aplicacao, depois todos os controllers. Facilita o estudo isolado de tecnologias, mas deixa o sistema sem fluxos completos por muito tempo e aumenta o risco de criar contratos antecipados.

### Por funcionalidades verticais

Completar cada funcionalidade de HTTP ate o banco antes de iniciar a seguinte. Gera valor executavel rapidamente, mas pode introduzir muitos conceitos novos simultaneamente para quem esta aprendendo.

### Hibrida — escolhida

Preparar apenas a fundacao necessaria e depois implementar funcionalidades verticais pequenas. Cada marco apresenta poucos conceitos novos, termina com comportamento executavel e reaproveita os contratos aprendidos nos marcos seguintes.

Esta abordagem foi escolhida por equilibrar aprendizado, progresso visivel e baixo risco de superengenharia.

## Ciclo de cada marco

Todo marco seguira a mesma sequencia:

1. Ler o requisito e reescreve-lo em linguagem de negocio.
2. Mapear o fluxo e as regras que pertencem ao dominio, aplicacao e adapters.
3. Estudar apenas os conceitos necessarios para aquele incremento.
4. Definir entradas, saidas, erros e autorizacao.
5. Escrever primeiro os testes do comportamento principal e das falhas criticas.
6. Implementar a menor solucao que satisfaca os testes e o requisito.
7. Executar testes e uma verificacao manual do endpoint quando aplicavel.
8. Revisar nomes, responsabilidades, seguranca e legibilidade.
9. Registrar o que foi aprendido e as decisoes tomadas.
10. Preparar respostas curtas para perguntas provaveis da entrevista.
11. Criar um commit pequeno e coerente depois da verificacao.

## Mapa geral dos marcos

### Marco 0 — Higiene e baseline

Garantir ambiente reproduzivel, segredo JWT valido, testes atuais verdes, Compose valido, README coerente e um ponto seguro no Git. Este marco nao adiciona funcionalidade de negocio.

### Marco 1 — Criacao e dados basicos da solicitacao

Construir o primeiro fluxo vertical do cliente: criar uma solicitacao `DRAFT`, salvar `serviceType`, `title` e `description`, persistir e retornar o progresso. Aqui serao consolidados identidade obtida do JWT, porta de persistencia, adapter JPA, DTOs e ownership.

### Marco 2 — Endereco e ViaCEP

Salvar a segunda etapa somente depois dos dados basicos, consultar ViaCEP, permitir numero e complemento fornecidos pelo cliente e tratar CEP inexistente ou indisponibilidade externa de forma previsivel.

### Marco 3 — Confirmacao e persistencia parcial

Salvar prioridade, data preferida, valor estimado e aceite dos termos. Validar data futura e a regra de prioridade alta. Permitir editar qualquer etapa somente enquanto a solicitacao estiver em `DRAFT`.

### Marco 4 — Submissao

Implementar `POST /solicitations/{id}/submit`, permitindo apenas ao cliente proprietario submeter um rascunho completo. O erro deve informar com clareza campos ou etapas faltantes. A transicao termina em `SUBMITTED`.

### Marco 5 — Administracao de usuarios internos

Implementar `POST /admin/users`. Somente `ADMIN` cria `ANALYST` ou `ADMIN`; analistas recebem o conjunto de UFs cobertas. Definir uma estrategia controlada para a criacao do primeiro administrador local.

### Marco 6 — Consulta e inicio da analise

Implementar a consulta do analista e o inicio da revisao. O backend deve validar perfil e cobertura da UF. Se o endpoint opcional de inicio for mantido, a transicao atomica sera `SUBMITTED -> IN_REVIEW`, com tratamento de concorrencia.

### Marco 7 — Decisao

Implementar aprovacao ou rejeicao com comentario de 10 a 1000 caracteres. Somente o analista atribuido decide uma solicitacao em `IN_REVIEW`. A decisao deve ser terminal e auditavel.

### Marco 8 — Indexacao e busca no Elasticsearch

Criar o documento de busca, mapping, porta e adapter. Indexar ao criar, salvar etapas, submeter e decidir. Implementar filtros por texto, status, tipo, prioridade, estado e intervalo de datas, alem de paginacao e ordenacao. A cobertura do analista sera sempre acrescentada pelo backend, independentemente da UF enviada na requisicao.

### Marco 9 — Consistencia por outbox

Conectar a tabela de outbox ao fluxo transacional e processar eventos de indexacao com retry. PostgreSQL permanece como fonte de verdade; falha temporaria do Elasticsearch nao desfaz uma alteracao de negocio ja confirmada.

### Marco 10 — Auditoria por AOP

Criar uma annotation pequena para os casos de uso relevantes e um aspect que grave ator, acao, recurso, resultado, request id, metadados e instante. O aspect nao deve conter regras de negocio nem registrar segredos.

### Marco 11 — Integracao, documentacao e observabilidade

Adicionar pelo menos um teste de integracao real, preferencialmente com Testcontainers, cobrindo API, PostgreSQL e Flyway. Completar OpenAPI, exemplos, README, metricas Micrometer e uma verificacao de inicializacao pelo Docker Compose. Uma pipeline simples de GitHub Actions pode ser adicionada depois do fluxo obrigatorio.

### Marco 12 — Revisao final e simulacao da entrevista

Executar todos os testes, exercitar os endpoints principais, revisar o repositorio limpo e simular perguntas sobre arquitetura, seguranca, transacoes, concorrencia, Elasticsearch, outbox, AOP e alternativas descartadas.

## Fronteiras arquiteturais

- `domain`: estado e invariantes que continuam verdadeiros independentemente de HTTP, banco ou framework.
- `application/port/in`: contratos dos casos de uso e seus comandos/resultados.
- `application/service`: orquestracao, transacao, autorizacao contextual e chamada das portas de saida.
- `application/port/out`: necessidades externas da aplicacao, como persistencia, CEP, token e indexacao.
- `adapter/in`: HTTP, DTOs, Bean Validation e traducao do principal autenticado.
- `adapter/out`: JPA, Elasticsearch, ViaCEP, BCrypt e demais detalhes externos.
- `shared`: somente elementos realmente compartilhados; nao deve se tornar um deposito generico.

As entidades de dominio podem continuar anotadas com JPA como escolha pragmatica do desafio. O custo dessa decisao deve ser conhecido: reduz mapeamentos, mas acopla o modelo ao mecanismo de persistencia.

## Estrategia de testes

- Testes de dominio para invariantes e transicoes de estado.
- Testes unitarios dos services com portas mockadas.
- Testes web para contrato HTTP, validacao, status e formato dos erros.
- Testes de adapters quando houver logica de traducao ou consulta.
- Pelo menos um teste de integracao real para migration, mapeamento JPA e fluxo principal.
- Testes de autorizacao para papel incorreto, recurso de outro cliente e UF fora da cobertura.
- Testes de concorrencia para inicio de analise quando aplicavel.

## Saida de aprendizado de cada marco

Os registros seguem as convencoes Teach: `MISSION.md` preserva o objetivo da jornada, `RESOURCES.md` mantem fontes confiaveis, `NOTES.md` guarda anotacoes temporarias e `learning-records/` registra somente aprendizados demonstrados.

Ao concluir um marco, o autor deve revisar as notas e produzir um resumo curto contendo:

- o problema resolvido;
- o fluxo entre as camadas;
- as regras protegidas;
- os testes que comprovam o comportamento;
- uma alternativa considerada e por que nao foi escolhida;
- duas ou tres perguntas provaveis da entrevista com respostas proprias.

O resumo so se torna um learning record quando houver evidencia de compreensao; atividade executada sem demonstracao nao conta automaticamente como aprendizado.

## Criterio de conclusao do projeto

O projeto estara pronto quando todos os endpoints obrigatorios estiverem executaveis; as regras de papel, dono, status e UF forem aplicadas no backend; a persistencia parcial e as transicoes estiverem testadas; a busca usar Elasticsearch; a indexacao for confiavel; a auditoria funcionar; o Compose iniciar o ambiente; houver teste de integracao real; e o autor conseguir explicar as decisoes sem depender de decorar o codigo.

## Decisoes deixadas para os planos de implementacao

- nomes e assinaturas exatas dos casos de uso de cada marco;
- formato exato dos endpoints de salvamento das tres etapas, conforme o documento completo;
- biblioteca e contrato HTTP do adapter ViaCEP;
- estrategia concreta de bootstrap do primeiro administrador;
- mapping e nome do indice Elasticsearch;
- politica de retry e agendamento da outbox;
- escopo exato do teste de integracao e da pipeline.
