# Change: alinhar documentação das rotas ao guard

**Data**: 2026-09-24 · **Tipo**: documentação operacional

## Intenção

Corrigir referências normativas e o comentário das rotas de solicitação que ainda exigiam `data.roles`. O `roleGuard`, as oito rotas protegidas e `routes-coverage.spec.ts` já usam `data.authorization`; esta alteração registra o contrato executado sem mudar o comportamento.

## Fronteira

- **EDIT**: `.specify/memory/constitution.md`, `frontend/src/app/features/solicitacao/solicitacao.routes.ts`.
- **CREATE**: este registro de mudança.
- **Fora de escopo**: rotas, guard, catálogo de ações e specs históricas.

## Risco e rollback

Risco de documentação incoerente se o contrato do guard mudar sem a mesma revisão. Reverter somente estes três arquivos restaura o texto anterior; não afeta execução.

## Tarefas

- [x] Corrigir as três referências do Princípio V e o comentário da rota.
- [x] Registrar Sync Impact Report e versão patch 2.1.1 da constituição.

## Gates

- [x] `npm test` (27 arquivos/126 testes, exit 0) e `npm run build` (exit 0) do frontend.
- [x] `py -m gems_ai check --project . --strict` (37 regras, conformant, exit 0).
- [ ] CI no SHA final da PR #35.
