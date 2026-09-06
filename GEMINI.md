# Contexto essencial do Gemini

Mantenha esta memória mínima: o Gemini a envia em todas as interações. Contexto de camada vive em
`backend/GEMINI.md`, `frontend/GEMINI.md`, `environments/GEMINI.md` e `specs/GEMINI.md`.

- Constituição: `.specify/memory/constitution.md`; leia-a integralmente apenas para governança,
  arquitetura, infraestrutura ou feature spec-driven.
- Código e testes consolidados vencem templates e documentos preliminares.
- Bug lê somente spec, contrato, código e teste afetados.
- Executor delegado lê somente pacote, contrato e lista fechada `READ`.
- Não pesquise `node_modules`, `target`, `dist`, `.angular` ou `business/**/*.png`.
- Não crie subagente para tarefa pequena, serial ou de arquivo único.
- Delegação zero é válida; retome a conversa existente sempre que possível.
- Nunca use `docker compose down -v` e nunca commite como executor delegado.

Gates finais: `backend/.\mvnw.cmd clean install`, `frontend/npm test` e
`frontend/npm run build`. No Windows, não use o script shell `./mvnw`.
