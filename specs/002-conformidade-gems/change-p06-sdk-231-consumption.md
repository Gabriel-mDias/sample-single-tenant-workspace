# Change: consumir SDK 2.3.1 e alinhar Keycloak

**Data**: 2026-09-24 · **Tipo**: ajuste técnico

## Intenção

Fixar a SDK publicada em `2.3.1`, cuja tag `v2.3.1` aponta para o merge da PR #16. A instalação revelou que a SDK requer `keycloak-angular@22`; o Sample declarava `16.1.0` e o npm instalou outra cópia aninhada. No navegador, o provedor da SDK e o `AuthService` local receberam tokens diferentes, causando `NG0201: No provider found for KeycloakService`. Alinhar a dependência raiz a `22.0.0` deduplica o token e restaura o login.

## Fronteira

- **EDIT**: `frontend/package.json`, `frontend/package-lock.json`.
- **CREATE**: este registro.
- **Fora de escopo**: código da autenticação, guard, rotas, backend e configuração do realm.

## Risco e rollback

Risco: mudança de major em `keycloak-angular`. O app iniciou, autenticou, exibiu ações, navegou em formulários e manteve os testes e build verdes no ensaio local. Reverter os dois manifestos e reinstalar restaura as versões anteriores, mas também a incompatibilidade com a SDK 2.3.1.

## Tarefas

- [x] Fixar SDK 2.3.1 e `keycloak-angular` 22.0.0 sem duplicata no grafo npm.
- [x] Reproduzir `NG0201` com a SDK 2.3.1 e Keycloak 16.1.0; validar login após alinhamento.

## Gates

- [x] `npm test` (126 testes), `npm run build` e `npm audit --package-lock-only --audit-level=high` (exit 0).
- [x] `py -m gems_ai check --project . --strict` (37 regras, exit 0).
- [ ] CI no SHA deste commit.

## Pendência encontrada

A SDK 2.3.1 ainda não abre o seletor de data por Espaço; a PR Angular GEMS SDK #17 prepara a correção para 2.3.2. Este registro não declara aceite de acessibilidade da P06.
