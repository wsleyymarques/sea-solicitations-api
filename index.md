# Repository index

Mapa rapido da API Spring Boot de solicitacoes, organizada como monolito modular com arquitetura hexagonal pragmatica.

<!-- picky-mayco:index:start -->
## Repository map

| Path | Responsibility | Main contents |
| --- | --- | --- |
| [`src/main/java/br/com/sea/solicitations/user/`](src/main/java/br/com/sea/solicitations/user/) | Cadastro, autenticacao e autorizacao de usuarios. | Dominio de usuario, casos de uso, portas e adapters HTTP, JPA, BCrypt e JWT. |
| [`src/main/java/br/com/sea/solicitations/solicitation/`](src/main/java/br/com/sea/solicitations/solicitation/) | Ciclo de vida e busca das solicitacoes de atendimento. | Agregado, casos de uso, contratos HTTP, persistencia JPA, busca Elasticsearch e integracao ViaCEP. |
| [`src/main/java/br/com/sea/solicitations/shared/`](src/main/java/br/com/sea/solicitations/shared/) | Infraestrutura e tipos compartilhados entre modulos. | Configuracao JWT e OpenAPI, seguranca HTTP, erros JSON e unidades federativas. |
| [`src/main/java/br/com/sea/solicitations/audit/`](src/main/java/br/com/sea/solicitations/audit/) | Auditoria das operacoes de escrita. | Annotation, aspect, extracao segura de contexto, writer transacional e persistencia JPA. |
| [`src/main/java/br/com/sea/solicitations/outbox/`](src/main/java/br/com/sea/solicitations/outbox/) | Indexacao assincrona confiavel das solicitacoes. | Evento e retry, porta e adapter JPA, claim com lock PostgreSQL, processador, poller e metricas. |
| [`src/main/resources/db/migration/`](src/main/resources/db/migration/) | Evolucao versionada do schema PostgreSQL. | Migrations SQL executadas pelo Flyway. |
| [`scripts/`](scripts/) | Utilitarios locais e verificacoes com infraestrutura real. | Bootstrap, testes em PostgreSQL descartavel e smoke Compose isolado. |
| [`frontend/`](frontend/) | Interface local para exercitar os fluxos da API. | Vite, formularios de cliente/admin/analista e proxy para a API. |
| [`postman/`](postman/) | Testes manuais da API por colecao importavel. | Colecao Postman e ambiente local sem credenciais reais. |
| [`src/test/java/br/com/sea/solicitations/`](src/test/java/br/com/sea/solicitations/) | Verificacao automatizada da aplicacao. | Testes de dominio, services, adapters, seguranca, controllers, metricas e fluxo HTTP com Testcontainers. |
| [`.github/workflows/`](.github/workflows/) | Integracao continua do repositorio. | Maven, build Vite, Testcontainers, validacao e smoke do Compose, build Docker. |
| [`.mvn/wrapper/`](.mvn/wrapper/) | Execucao reproduzivel do Maven. | Configuracao do Maven Wrapper usada localmente e no CI. |
| [`docs/superpowers/specs/`](docs/superpowers/specs/) | Decisoes e desenhos aprovados das funcionalidades. | Especificacoes de arquitetura e comportamento. |
| [`docs/superpowers/plans/`](docs/superpowers/plans/) | Decomposicao executavel das implementacoes. | Planos por tarefa, arquivos afetados e comandos de verificacao. |
| [`docs/reviews/`](docs/reviews/) | Resultado da revisao final. | Evidencias de teste, riscos residuais e perguntas de entrevista. |

<!-- picky-mayco:index:end -->
