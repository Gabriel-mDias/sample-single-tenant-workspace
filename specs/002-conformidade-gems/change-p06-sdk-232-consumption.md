# Change: consumir correção de teclado da SDK 2.3.2

**Data**: 2026-09-24 · **Tipo**: ajuste técnico

## Intenção

Atualizar a versão exata de `@gabriel-mdias/angular-gems-sdk` de 2.3.1 para 2.3.2. A 2.3.1 corrigiu os três achados Lighthouse, mas o ícone de data continuou sem resposta a Espaço no navegador. A tag `v2.3.2` contém a correção testada na SDK; a auditoria autenticada no Sample confirmou Espaço e Enter.

## Fronteira

- **EDIT**: `frontend/package.json`, `frontend/package-lock.json`.
- **CREATE**: este registro.
- **Fora de escopo**: código de aplicação, guard, rotas, backend e realm.

## Risco e rollback

Risco de regressão de dependência; os testes, build e navegador são repetidos no pacote publicado. Reverter os dois manifestos e executar `npm ci` retorna à 2.3.1 e ao defeito de teclado.

## Tarefas

- [x] Confirmar release e tarball publicados no GitHub Packages.
- [x] Fixar 2.3.2 e confirmar uma instância de `keycloak-angular@22.0.0`.
- [x] Repetir Lighthouse autenticado nas três telas e testar o seletor de data com Espaço/Enter.

## Gates

- [x] `npm test` (126 testes) e `npm run build`, ambos exit 0.
- [ ] Auditoria de dependências, toolkit estrito e CI no SHA final.

O aceite global da P06 requer ainda a matriz dos 12 invariantes, 11 pesos e zero crítico demonstrado.
