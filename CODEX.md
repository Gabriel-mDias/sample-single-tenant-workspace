# CODEX.md — roteador sob demanda

`AGENTS.md` é a entrada automática do Codex. Este arquivo não deve ser lido em toda tarefa; consulte-o
quando a classificação abaixo exigir.

## Autoridade

1. `.specify/memory/constitution.md` — autoridade máxima.
2. seção manual de `AGENTS.md` — regras transversais locais.
3. spec aprovada da feature — comportamento pretendido.
4. código e testes — estado real e referência de implementação.
5. regras GEMS aplicáveis — convenções da camada, subordinadas ao código consolidado.

Conflito entre spec aprovada e código deve ser investigado. Código vence somente documentos
preliminares e templates divergentes.

## Leitura proporcional

| Tarefa | Leia |
| :--- | :--- |
| Resposta ou diagnóstico | somente arquivos diretamente relevantes |
| Bug | spec/contrato afetado, implementação e teste de regressão |
| Backend | `backend/AGENTS.md`, regra GEMS da camada e referência equivalente |
| Frontend | `frontend/AGENTS.md`, regra GEMS da tela e referência equivalente |
| Ambiente | `environments/AGENTS.md` e arquivos do ambiente afetado |
| Feature/mudança de comportamento | skill `spec-driven` e artefatos da feature |
| Arquitetura/governança | constituição completa, grafo atual e documentos diretamente afetados |
| Executor de lote | pacote, contrato do executor e lista fechada `READ`; nada além |

Não carregue automaticamente specs concluídas, `business/`, planos de ondas anteriores, templates,
skills de outras plataformas ou o contexto completo do projeto.

## Fluxos

- Feature: `specify → clarify → plan → tasks → analyze → roteiro → implement → verify`.
- Bug contratado: teste de regressão → correção → gate proporcional.
- Mudança pequena sem comportamento: `change.md` curto → tarefas → gate proporcional.
- Executor headless: contrato em
  `.claude/skills/delegacao-de-modelos/references/contrato-do-executor.md`.

## Overrides recorrentes

- REST retorna tipo direto; IDs Java são `UUID`.
- `setBodyToExceptionLog(dto, request)` exige `HttpServletRequest`.
- Single-tenant; sem cascade; ambiente e Liquibase somente em `app-deploy`.
- Autorização por uma ação concreta; erro do Keycloak é 502.
- Situação de fluxo nunca vem do corpo — cada transição é um endpoint de ação próprio.
- Frontend Angular standalone e Vitest; raiz registra features por `loadChildren`.

Os detalhes permanecem nas instruções aninhadas e no código de referência, para não serem cobrados em
toda sessão.

## Gates

```powershell
cd backend; .\mvnw.cmd clean install
cd ..\frontend; npm test; npm run build
```

Use gates focados durante a iteração e o gate completo uma única vez no fechamento. Nunca execute
`docker compose down -v`.

⚠️ **No Windows o Codex executa via `powershell.exe`, e `./mvnw` é script `sh`.** Use
`cmd /c mvnw.cmd ...` — o script shell falha na invocação mesmo com o código correto.
