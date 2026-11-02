# Repository index

Mapa rapido da API Spring Boot de solicitacoes, organizada como monolito modular com arquitetura hexagonal pragmatica.

<!-- picky-mayco:index:start -->
## Repository map

| Path | Responsibility | Main contents |
| --- | --- | --- |
| [`src/main/java/br/com/sea/solicitations/user/`](src/main/java/br/com/sea/solicitations/user/) | Cadastro, autenticacao e autorizacao de usuarios. | Dominio de usuario, casos de uso, portas e adapters HTTP, JPA, BCrypt e JWT. |
| [`src/main/java/br/com/sea/solicitations/solicitation/`](src/main/java/br/com/sea/solicitations/solicitation/) | Ciclo de vida das solicitacoes de atendimento. | Agregado, casos de uso, contratos HTTP, persistencia JPA e integracao ViaCEP. |
| [`src/main/java/br/com/sea/solicitations/shared/`](src/main/java/br/com/sea/solicitations/shared/) | Infraestrutura e tipos compartilhados entre modulos. | Configuracao JWT, seguranca HTTP, erros JSON e unidades federativas. |
| [`src/main/java/br/com/sea/solicitations/audit/`](src/main/java/br/com/sea/solicitations/audit/) | Modelo de auditoria das operacoes relevantes. | Entidade e conceitos de log de auditoria. |
| [`src/main/java/br/com/sea/solicitations/outbox/`](src/main/java/br/com/sea/solicitations/outbox/) | Fundacao para publicacao confiavel de eventos. | Entidade de evento outbox e seu estado. |
| [`src/main/resources/db/migration/`](src/main/resources/db/migration/) | Evolucao versionada do schema PostgreSQL. | Migrations SQL executadas pelo Flyway. |
| [`src/test/java/br/com/sea/solicitations/`](src/test/java/br/com/sea/solicitations/) | Verificacao automatizada da aplicacao. | Testes de dominio, services, adapters, seguranca, controllers e contexto Spring. |
| [`.github/workflows/`](.github/workflows/) | Integracao continua do repositorio. | Workflow que executa verificacao Maven, validacao do Compose e build Docker. |
| [`.mvn/wrapper/`](.mvn/wrapper/) | Execucao reproduzivel do Maven. | Configuracao do Maven Wrapper usada localmente e no CI. |
| [`docs/superpowers/specs/`](docs/superpowers/specs/) | Decisoes e desenhos aprovados das funcionalidades. | Especificacoes de arquitetura e comportamento. |
| [`docs/superpowers/plans/`](docs/superpowers/plans/) | Decomposicao executavel das implementacoes. | Planos por tarefa, arquivos afetados e comandos de verificacao. |

<!-- picky-mayco:index:end -->
