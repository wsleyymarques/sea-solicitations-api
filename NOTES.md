# Notas de estudo

## Objetivo atual

Concluir o Marco 0: obter um baseline local reproduzivel antes de iniciar os casos de uso de solicitacao.

## Proxima verificacao

Desenhar como os casos de uso receberao a identidade autenticada para validar dono e cobertura de UF, complementando o RBAC de rotas.

## Evidencias temporarias

- O Git ignora `.env` pela regra declarada na linha 27 de `.gitignore`.
- O segredo anterior decodificava para 24 bytes, abaixo do minimo exigido.
- O novo segredo local decodifica corretamente para 32 bytes e seu conteudo nao foi exposto.
- Os 9 testes de `JwtProperties` e `JwtConfiguration` passaram com Java 21, sem falhas ou erros.
- O README agora distingue o fluxo vertical implementado de usuarios das fundacoes de dominio ainda incompletas.
- Os links locais da documentacao existem e `git diff --check` nao encontrou erros de whitespace.
- O RBAC de rotas permite `/admin/**` para `ADMIN`, `/analyst/**` para `ANALYST` e `/solicitations/**` para `CLIENT`.
- A compilacao e os 54 testes passaram depois da configuracao do RBAC.
- RBAC resolve o papel geral, mas dono, UF, analista atribuido e estado permanecem como autorizacao contextual nos casos de uso e no dominio.

## Regra de registro

Este arquivo guarda anotacoes temporarias da jornada. Aprendizados nao obvios somente entram em `learning-records/` depois que forem demonstrados em uma tentativa, explicacao ou exercicio concluido.
