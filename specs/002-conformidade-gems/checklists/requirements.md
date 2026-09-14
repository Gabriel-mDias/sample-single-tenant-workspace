# Checklist de requisitos — Conformidade GEMS

**Data**: 2026-09-14 · **Spec**: [spec.md](../spec.md)

## Conteúdo e linguagem

- [x] Histórias descrevem ator, valor e comportamento observável sem prescrever implementação.
- [x] Cenários são verificáveis em termos de contrato.
- [x] Requisitos técnicos nomeiam mecanismos apenas quando definem conformidade.
- [x] Critérios medem resultado e não afirmam implementação existente.
- [x] Escopo preservado e caminhos condicionais estão explícitos.

## Cobertura

- [x] Nove princípios constitucionais têm requisito/gate.
- [x] S-12 exige zero Java, remoção das 22 supressões e do ratchet.
- [x] Frontend exige mínimo medido e decisão humana nominal por residual.
- [x] Auditoria exige bloco atômico, identidade estável e correlação.
- [x] Keycloak cobre o ciclo atual, roles/compostas e compensações.
- [x] Contagens atuais: 4 repositories, 2 controllers, 4 handlers, 6 mappers e 177 nomes.
- [x] Rollback exige Spring integrado, proxy, banco real e ausência de dado/efeito/trilha.

## Clarificação

- [ ] D-P01 autoria estável/correlação decidida pelo product owner.
- [ ] D-P02 superfície Keycloak completa decidida pelo product owner.
- [ ] D-P03 expansão de ambiente decidida se a alternativa declarativa for escolhida.
- [ ] D-P04 residuais frontend decididos depois da medição final.

## Resultado

**CONDITIONAL PASS**: especificação completa e rastreável; quatro decisões continuam humanas. Este
checklist não concede aprovação nem autoriza implementação.
