# Checklist de segurança — Conformidade GEMS

## Garantias planejadas

- [x] Endpoint público/global/tenant tem classificação única e scan sobre mappings reais.
- [x] Perfil genérico não protege endpoint; ação concreta deve existir no catálogo.
- [x] Catálogo frontend deriva do enum e o gate reprova divergência.
- [x] Guard/menu/tela falham fechados; backend permanece autoridade.
- [x] Auditoria exige identidade estável e correlação; login atual não substitui identidade.
- [x] Sigilo, commit/rollback e ator técnico têm testes/mutações locais.
- [x] Keycloak exige preservar ciclo completo, grupos, roles, compostas e compensações.
- [x] Nenhum adapter recria por fora a API ausente da SDK.
- [x] Realm declarativo, se escolhido, preserva clients, mappers, users, groups, defaults e client roles.
- [x] Quatro handlers só saem com equivalência do envelope/status e órfãos no mesmo bloco.
- [x] Zero exceção GEMS nova; agente não aceita residual ou perda.

## Resultado

**CONDITIONAL PASS** por D-P01/D-P02/D-P03. Segurança não está declarada PASS enquanto as perdas
potenciais de autoria, correlação e ciclo Keycloak não forem decididas por humano.
