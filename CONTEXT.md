# Solicitation Management

Este contexto recebe solicitacoes de atendimento de clientes e controla seu preenchimento, submissao e analise por usuarios internos.

## Language

**User**:
Pessoa autenticada no sistema, identificada por um unico perfil de acesso.
_Evitar_: Account, login

**Client**:
Perfil de usuario que cria, edita e submete apenas as proprias solicitacoes.
_Evitar_: Customer, requester

**Analyst**:
Perfil de usuario interno que analisa solicitacoes pertencentes as UFs sob sua cobertura.
_Evitar_: Reviewer, attendant

**Administrator**:
Perfil de usuario interno que cria outros usuarios internos e atribui UFs aos analistas.
_Evitar_: Manager, superuser

**Solicitation**:
Pedido de atendimento pertencente a um cliente, preenchido em etapas e posteriormente decidido por um analista.
_Evitar_: Request, ticket, order

**Stage**:
Parte sequencial do preenchimento de uma solicitacao: dados basicos, endereco ou confirmacao.
_Evitar_: Form, page

**Coverage**:
Conjunto de UFs atribuidas a um analista e que limita quais solicitacoes ele pode acessar e analisar.
_Evitar_: Region, jurisdiction

**Decision**:
Resultado terminal da analise de uma solicitacao, acompanhado do analista, comentario e momento da decisao.
_Evitar_: Evaluation, review result
