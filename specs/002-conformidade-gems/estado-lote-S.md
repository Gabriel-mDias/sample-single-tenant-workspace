# Estado — Lote S, rodada 002

| Controle | Valor |
| :--- | :--- |
| Checkpoint | `C00` |
| Pré-requisitos | `nenhum` |
| Política de sessão | `nova` |
| SHA anterior | `c1a466b` |
| Estado inicial | `em recuperação` |
| Estado final | `verde` |
| Gate focado | `backend: dependency:tree + app-core -am test; frontend: npm ci; git diff --check` |
| Ponto de recuperação | `C00 fechado verde; retomar por C01. T008 segue pendente para o gate Docker de C10.` |

## 1. Rollback deste lote

Restaurar nominalmente apenas os documentos editáveis deste checkpoint; preservar os cinco
manifests e o pacote recebidos antes desta sessão:

```powershell
git checkout -- 'specs/002-conformidade-gems/tasks.md' 'specs/002-conformidade-gems/roteiro.md'
```

O estado é o registro protocolar da execução e permanece disponível para recuperação.

## 2. Concluído

| Tarefa | O que provou |
| :-- | :--- |
| T005 | Revalidado em C00: `backend: .\mvnw.cmd dependency:tree` exit 0, BUILD SUCCESS, seis módulos do reator; GEMS 3.1.0 e ModelMapper 3.2.4 resolvidos. |
| T006 | Revalidado em C00: GEMS 3.1.0, processadores compile/testCompile e `gems-mapstruct` somente em `app-core`; ModelMapper 3.2.4 fixado temporariamente no pai. `backend: .\mvnw.cmd -pl app-core -am test` exit 0, BUILD SUCCESS; 13 testes security + 132 core, zero falhas/erros. |
| T007 | Revalidado em C00: Angular GEMS `^2.1.0` e overrides de common/core/router; `npm ci` sem flag exit 0, 392 pacotes instalados e 0 vulnerabilidades. |
| C00 | `git diff --check` exit 0; somente aviso de normalização CRLF/LF em `backend/pom.xml`. `tasks.md` e a linha de implementação do §9 de `roteiro.md` atualizados no escopo autorizado. |

## 3. Em curso

Nenhuma tarefa em curso. C00 está fechado verde.

## 4. Não iniciado

T008 permanece `[ ]`, com implementação preservada e `docker build --target build .` remetido a C10.
C01 é o próximo checkpoint; T009–T012 não foram iniciadas.

## 5. Arquivos tocados

`git status --porcelain --untracked-files=all`

```text
 M backend/app-core/pom.xml
 M backend/pom.xml
 M frontend/Dockerfile
 M frontend/package-lock.json
 M frontend/package.json
 M specs/002-conformidade-gems/roteiro.md
 M specs/002-conformidade-gems/tasks.md
?? specs/002-conformidade-gems/pacote-de-execucao-S.md
```

## 6. Decisões fora do pacote

Nenhuma decisão técnica foi tomada fora do pacote. O pino temporário de
`org.modelmapper:modelmapper:3.2.4` recebido foi preservado para remoção em T051.

## 7. O que está quebrado agora

Nada quebrado pelo C00: todos os gates focados passaram. O gate Docker de T008 não foi executado
neste checkpoint e permanece explicitamente pendente para C10.

## 8. Retomada

- **Executor:** Codex
- **Política:** sessão `nova`; o identificador legado é apenas diagnóstico e não foi reutilizado.
