# Desenho da Administracao de Usuarios Internos

## Intencao

Permitir que um administrador autenticado crie os usuarios internos usados nos
fluxos de administracao e analise, concluindo o Marco 5. O endpoint nunca cria
clientes e nunca devolve senha ou hash.

## Contrato HTTP

- Metodo e caminho: `POST /admin/users`.
- Acesso: somente JWT com `ROLE_ADMIN`.
- Corpo: `name`, `email`, `password`, `role` e `coveredStates`.
- `role` aceita somente `ANALYST` ou `ADMIN`.
- `ANALYST` exige pelo menos uma UF em `coveredStates`.
- `ADMIN` aceita `coveredStates` ausente ou vazio e rejeita cobertura nao vazia.
- Resposta: `201 Created` com `id`, `name`, `email`, `role` e
  `coveredStates`.

Nome e e-mail seguem a normalizacao do agregado `User`. A senha usa as mesmas
restricoes do cadastro de cliente (8 a 72 caracteres) e e protegida pelo
adapter BCrypt existente antes da persistencia.

## Erros

- `400 VALIDATION_ERROR`: campos HTTP ausentes, malformados ou fora dos
  limites.
- `400 INVALID_REQUEST`: papel `CLIENT` ou combinacao invalida entre papel e
  cobertura.
- `403 FORBIDDEN`: usuario autenticado sem perfil `ADMIN`.
- `409 EMAIL_ALREADY_REGISTERED`: e-mail ja utilizado por qualquer usuario.

## Arquitetura

Um controller administrativo traduz o JSON para `CreateInternalUserCommand`.
O command protege as invariantes de papel/cobertura e redige a senha no
`toString`. O service reutiliza `UserRepository`, `PasswordHasher` e o agregado
`User`, atribui as UFs somente ao analista e executa tudo em transacao. O
adapter JPA existente persiste o `ElementCollection` em `analyst_coverages`.

## Abordagens consideradas

1. **Novo caso de uso sobre o modulo `user` — escolhida.** Reutiliza as portas
   e o agregado existentes sem duplicar persistencia ou BCrypt.
2. Estender `RegisterClientService` com um parametro de papel. Foi descartada
   porque mistura cadastro publico com administracao privilegiada e aumenta o
   risco de criar papel interno pela rota publica.
3. Insercao direta por SQL no controller. Foi descartada por expor detalhes de
   persistencia, ignorar invariantes do dominio e duplicar hashing.

## Primeiro administrador

O bootstrap local ja existente em `scripts/bootstrap-admin.sh` permanece como
estrategia controlada para criar o primeiro administrador. Depois disso, novos
usuarios internos sao criados exclusivamente pelo endpoint autenticado.

## Nao objetivos

- Alterar, desativar, listar ou excluir usuarios internos.
- Redefinir senhas.
- Criar clientes pela rota administrativa.
- Emitir token automaticamente apos a criacao.

## Verificacao

Testes unitarios cobrem papeis, cobertura, duplicidade e hashing. Testes web
cobrem validacao, resposta sem senha e RBAC real. Um teste opt-in executa a
persistencia do analista e de suas UFs contra PostgreSQL descartavel. A suite
Maven completa deve permanecer verde.
