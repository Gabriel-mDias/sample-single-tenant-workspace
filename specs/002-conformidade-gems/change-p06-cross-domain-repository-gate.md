# Change: reprovar escrita direta em repository de outro domínio

**Data**: 2026-09-24 · **Tipo**: ajuste técnico de governança

## Intenção

O Princípio I permite consulta por repository de outro domínio, mas exige evento para escrita. O gate P05 detectava service→service e deixava passar `outroDominioRepository.save/delete`. O novo gate JavaParser examina imports e campos de repository cruzados e reprova chamadas diretas de `save`, `delete`, `update`, `insert` e `flush`, preservando as consultas atuais.

## Fronteira

- **EDIT**: `backend/app-deploy/src/test/java/br/com/gems/app/convencoes/{ConvencoesFonteTest,RegrasDeFonte,ExcecoesEscritasTest}.java`, `.specify/memory/constitution.md`, `AGENTS.md` raiz e quatro `AGENTS.md` de camada.
- **CREATE**: este registro.
- **Fora de escopo**: services, repositories, eventos de produção e regras geradas do toolkit.

## Risco e rollback

O gate reconhece chamadas diretas a campos tipados por imports explícitos; aliases, escrita indireta em entidade lida e queries mutáveis customizadas ainda exigem inspeção. Reverter os arquivos listados restaura o gate e a redação anteriores, sem alterar comportamento de produção.

## Tarefas

- [x] Inventariar todos os imports cruzados de repository em produção; os três services encontrados apenas consultam.
- [x] Acrescentar a regra de fonte e incluí-la na reconciliação de exceções.
- [x] Atualizar Sync Impact Report e versão patch 2.1.2 da constituição; alinhar AGENTS.md.

## Gates

- [x] Baseline `ConvencoesFonteTest`: 10 testes, exit 0.
- [x] Mutação temporária `parceiroRepository.deleteById(...)` em `SolicitacaoService.insert`: teste reprovou com `events.md#1`, arquivo/linha e exit 1.
- [x] Remoção da mutação e repetição: exit 0.
- [x] Reator Maven `clean install` após encerrar o JAR local: 32 relatórios/215 testes, 0 falhas, exit 0.
- [x] Toolkit `check --strict` (37 regras) e contexto de agentes, ambos exit 0.
- [ ] CI no SHA desta mudança.
