# Checklist técnico — Conformidade GEMS

## Planejamento verificável

- [x] Constitution Check usa os nove títulos reais e avalia todos.
- [x] PASS de desenho está separado de decisão humana pendente.
- [x] SDKs/releases/fontes públicas foram aferidas; API ausente não foi presumida.
- [x] Auditoria forma um bloco atômico sem checkpoint intermediário publicável.
- [x] Rollback usa bean proxied, PostgreSQL real, exceção checked após escrita e teste sem transação própria.
- [x] Quatro repositories reais, dois controllers, quatro handlers, seis mappers e 177 nomes estão identificados.
- [x] Arquivos novos são marcados CREATE; remoções e órfãos são nominais.
- [x] Cada fase tem comando focado e condição verde.
- [x] Java fecha em zero, sem 22 supressões de dívida nem Spotless ratchet.
- [x] Frontend fecha no mínimo medido, com residual sujeito a humano.
- [x] Todos os locais operacionais atuais da flag de peers foram apurados; expansão T004 aprovada.
- [x] Realm one-shot/cache/dependências e preservação das seções não derivadas foram apurados.

## Decisões

- [x] D-P01 preserva auditoria e exige release compatível antes da adoção.
- [x] D-P02 preserva Keycloak e exige release compatível antes da substituição.
- [x] D-P03 não aplicável; ambiente/realm preservados.
- [ ] D-P04 aceita cada residual frontend depois da medição.

## Resultado

**PASS DE PORTÃO**. O plano técnico está aprovado; auditoria e Keycloak não podem começar antes das
releases compatíveis, e D-P04 permanece para a medição final.
