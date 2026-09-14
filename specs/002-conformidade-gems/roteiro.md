# Roteiro da Rodada — 002 Conformidade GEMS

**Feature**: `002-conformidade-gems` · **Branch**: `feature/conformidade-gems` · **Requisito**: prova S-codex da rodada 010
**Data**: 2026-09-14 · **Status**: aguardando aprovação humana do planejamento revisado

> Este documento apresenta escopo, decisões, impactos, testes e ordem da rodada. Quatro decisões
> continuam humanas; nenhuma está assinada e nenhuma implementação está autorizada.

**Índice**: [1 Objetivo](#1-objetivo-da-rodada) · [2 Decisões](#2-decisões-de-entrada-e-pendentes) ·
[3 Banco](#3-banco-de-dados) · [4 Backend](#4-backend) · [5 Frontend](#5-frontend) ·
[6 Testes](#6-testes-esperados-por-bloco) · [7 Impacto](#7-interação-e-isolamento) ·
[8 Tasks](#8-divisão-de-tasks-e-delegação-por-modelo) · [9 SDD](#9-prompts-sdd-da-rodada) ·
[10 Riscos](#10-riscos-e-mitigações) · [11 Fora](#11-fora-de-escopo) ·
[12 Aprovação](#12-critérios-de-aprovação)

---

## 1. Objetivo da rodada

Tornar o sample coerente com a constituição 2.0.0, regras GEMS 2.4.0 e módulos publicados, preservando
as features de referência. O backend termina sem dívida tolerada; o frontend termina no mínimo medido,
e qualquer residual continua pendente até aceite humano nominal.

### Por que agora

S-def instalou definições/gates e T058 confirmou o reator verde. S-codex precisa transformar o molde
em exemplo conforme: reduzir apenas totais não satisfaz S-12, e adotar SDK sem equivalência viola a
própria constituição.

### O que a rodada carrega

Medição de grafo; MapStruct em seis services; rollback integrado em dois fluxos; auditoria atômica;
autorização/Keycloak; Angular 2.1.0; quatro repositories, dois controllers, quatro handlers, 177 nomes;
remoção das 22 supressões de dívida e do Spotless ratchet; medição/decisão de residual frontend.

---

## 2. Decisões de entrada e pendentes

### 2.1 Entradas aprovadas na fase Start

| # | Ponto | Decisão |
| :-- | :--- | :--- |
| 1 | Condução | Codex/Sol conduz a prova S-codex dentro da 010, sem Terra. |
| 2 | Autoridade | Constituição 2.0.0 e regras 2.4.0 vencem código; `medicao-gates.md` vence histórico. |
| 3 | Fronteira | Preservar parceiro, solicitação, localidade, normalização, usuário, auditoria e S3. |
| 4 | Java | Adotar versão publicada, uma propriedade pai e árvores antes/depois. |
| 5 | Angular | Adotar 2.1.0/overrides e retirar a flag operacional de peers. |
| 6 | Mapping | Seis services em MapStruct/GemsMappingConfig estrito. |
| 7 | Transação | Spring `rollbackFor`, com mutação nas duas exceções. |
| 8 | Auditoria | SDK só entra com tela/consulta, autoria, correlação, agregados, sigilo, busca e data preservados; escrita local sai junto. |
| 9 | Keycloak/autorização | SDKs só substituem clientes locais com catálogo, bootstrap, perfis e ciclo de usuário preservados. |
| 10 | Backend | Controllers, repositories, services, handlers, OpenAPI, eventos, persistência, validação, observabilidade e S3 conformes; agente não cria exceção. |
| 11 | Frontend | `isAuthorized`, quatro paths por feature, `shared`, componentes SDK, forms/signals/stores/tokens. |
| 12 | Gates | T057 é ponto inicial; S-12 exige Java zero e frontend mínimo medido, não mera redução. |
| 13 | Baseline | T058: 204 backend e 21/118 frontend verdes. |
| 14 | Dependências | D-01/D-02 e labels não são reabertas. |
| 15 | Tenant | O sample permanece single tenant. |
| 16 | Fora | ADACI, Keycloak Angular 22, TS7 e promoção de commons ficam fora. |
| 17 | Prova 010 | Silent check-sso recebe crédito OK-1; roadmap OK-2 no fechamento. |

### 2.2 Clarificações que este portão precisa resolver

| ID | Evidência | Opções | Recomendação |
| :--- | :--- | :--- | :--- |
| D-P01 | Auditing 3.1.0 não grava identidade estável/correlação. | Release SDK prévia ou emenda aceitando perda. | **Preservar e bloquear até release publicada.** |
| D-P02 | Keycloak gateway 3.1.0 não cobre update/enable/disable/delete/compensação/grupos completos nem criar/compor roles. | Release SDK prévia ou emenda de contrato. | **Preservar e bloquear até release publicada.** |
| D-P03 | Realm declarativo altera ambiente fora do EDIT original. | Não alterar; ou expandir e derivar só roles/compostas. | Condicional à alternativa de D-P02; preferir release e manter ambiente. |
| D-P04 | Mínimo frontend só existe depois da correção/medição. | Aceitar ou recusar cada residual. | Deixar desmarcado até inventário final. |

---

## 3. Banco de dados

O schema atual preserva `ID_AUTOR` e `CD_CORRELACAO`. A SDK 3.1.0 escreve somente nome/tipo do ator;
colunas extras não resolvem porque o writer não as preenche. Resolver id por login atual falha após
rename/delete/reuso e foi retirado do plano.

### Bloco de auditoria

Depois de D-P01 e de nova API publicada aferida, migration, read adapter, provider, configuração,
ativação e remoção dos escritores locais entram em **uma única alteração**. Testes de upgrade são
escritos primeiro; backfills precedem constraints; dado incompatível para a migração; índices/FKs são
recriados; `ddl-auto: validate` fecha verde. Não existe checkpoint com tabela e writer incompatíveis.

Se o PO escolher perda semântica, a §12 precisa nomear que linhas novas não terão identidade estável
nem correlação e que o filtro por autor ficará incompleto. Este roteiro não registra esse aceite.

---

## 4. Backend

### 4.1 Dependências e mapping

POM pai concentra versão/processadores. Entram seis mappers nominais para localidade, contato,
endereço, pessoa, parceiro e solicitação. `ModelMapper` sai quando a última referência sair.

### 4.2 Transações

Parceiro e solicitação usam transação Spring com `rollbackFor`. A prova vive em `app-deploy`, chama
bean proxied contra PostgreSQL e lança checked após escrita. O teste não é transacional; nova leitura
afirma dado, filhos, efeito e trilha ausentes. Retirar `rollbackFor` precisa persistir algo e falhar.

### 4.3 Conformidade nominal

| Dívida atual | Alvo final |
| :--- | :--- |
| 314 Checkstyle em 22 supressões | zero; supressões de dívida removidas |
| repositories | exatamente CampoAlterado, Estado, Pais e EtapaSolicitacao corrigidos |
| controllers | Localidade e Cep em BaseController |
| ordens | seis services corrigidos |
| nomes | 177 identidades `deveX_quandoY` |
| Spotless | `spotless:apply`, rechecagem total e `ratchetFrom` removido |

### 4.4 Handlers e órfãos

Saem juntos `ProvedorExternoExceptionHandler`, `AccessDeniedExceptionHandler`,
`IdentityProviderExceptionHandler` e `RequisicaoInvalidaExceptionHandler`, após testes de envelope.
`ProvedorExternoException` adota a base externa publicada. `PublicEndpoint`, `PublicRoute`,
`CreateUserDTO` e outros tipos saem somente quando busca nominal provar zero consumidor; `SecurityRoles`
e `PasswordUtil` permanecem enquanto tiverem uso real.

### 4.5 Autorização/Keycloak

Catálogo/scan da SDK são compatíveis. A remoção das quatro classes administrativas fica bloqueada por
D-P02: o gateway aferido não preserva todo o ciclo. Um adapter novo não será criado para contornar.

---

## 5. Frontend

### 5.1 Forma e comportamento

Parceiro e solicitação terão `list`, `form`, `form/:id`, `view/:id`. Os 12 arquivos medidos deixam
`hasRole`/`hasAnyRole`; sete componentes deixam navegação direta onde a SDK se aplica; dez arquivos
`commons` ganham destinos CREATE nominais em `shared`; field-error local sai após equivalência.

### 5.2 Instalação e expansão

SDK 2.1.0/overrides permitem `npm ci` sem flag. Os pontos operacionais atuais estão em
`frontend/Dockerfile`, `.github/workflows/ci.yml`, `README.md` e `.github/dependabot.yml`. Os três
últimos exigem expansão do escopo de implementação. Menções normativas em constituição/regras são
imutáveis e reportadas, não alteradas por conveniência.

### 5.3 Mínimo medido

O gate mede visual, testes, signals e organização por identidade. O objetivo é zero quando aplicável.
Qualquer residual vem com arquivo, regra, motivo e alternativa e continua desmarcado até D-P04. O
agente não transforma contagem menor em autorização.

---

## 6. Testes esperados por bloco

| Bloco | Prova | Gate |
| :--- | :--- | :--- |
| Dependências | árvores antes/depois; host/Docker/CI sem flag | `dependency:tree`, `npm ci`, Docker build |
| Keycloak/autorização | 21 ações, scan, ciclo completo, compensações | teste focado T024; bloqueado por D-P02 |
| Auditoria atômica | upgrade, commit/rollback, sigilo, identidade/correlação, consulta | teste focado T035 + mutações T036 |
| Mapping/transação | seis mappers; proxy/banco/checked sem falso rollback | T050 + mutações T048/T049 |
| Java zero | quatro repos, dois controllers, seis ordens, 177 nomes, 314/22, ratchet | T052–T061 |
| Frontend | auth, oito rotas, SDK/shared, 70 cores, residual nominal | T072/T073 e gate completo |
| Fechamento | todas as redes mutadas e quickstart | T074–T079 |

Baseline reutilizado nesta fase: backend exit 0/204 (13 security, 132 core, 59 deploy) e frontend exit
0/21 arquivos/118 testes. Não foi repetido porque não houve alteração de código.

---

## 7. Interação e isolamento

**Parcialmente isolada.** A rodada preserva domínio, mas altera build, persistência, segurança,
ambiente condicional e UI compartilhada. A atomicidade da auditoria e os gates completos limitam a
exposição.

| Recurso | Impacto | Garantia |
| :--- | :--- | :--- |
| POM/build Java | todos os módulos | árvores e clean install; zero/ratchet removido |
| auditoria | toda entidade auditável | bloco único e D-P01 |
| Keycloak | usuário/perfis/endpoints | D-P02 e testes completos |
| realm condicional | local/staging/prod | D-P03, one-shot e seções não derivadas preservadas |
| Angular/shared | shell e duas features | testes focados, 21/118 + novos e D-P04 |

Não há violação constitucional justificada. Pendência humana não é exceção nem Complexity Tracking.

---

## 8. Divisão de tasks e delegação por modelo

**79 tarefas em 8 fases**: decisões 4; dependências 8; autorização/Keycloak 12; auditoria atômica 12;
mappers/transações 15; Java zero 10; frontend 12; fechamento 6.

| Fase | Tasks | Modelo | Gate |
| :--- | :--- | :--- | :--- |
| 0 decisões | T001–T004 | Humano + Sol registra | decisões/expansões explícitas |
| 1 dependências | T005–T012 | Sol/Codex | resolução publicada e sem flag operacional |
| 2 auth/Keycloak | T013–T024 | Sol/Codex | contrato completo; bloqueado D-P02 |
| 3 auditoria | T025–T036 | Sol/Codex | um gate atômico; bloqueado D-P01 |
| 4 mapping/rollback | T037–T051 | Sol/Codex | seis mappers e dois rollbacks reais |
| 5 Java zero | T052–T061 | Sol/Codex | zero Java e ratchet removido |
| 6 frontend | T062–T073 | Sol/Codex | mínimo medido; D-P04 humano |
| 7 fechamento | T074–T079 | Sol/Codex | gates/quickstart/analyze |

Sem delegação para Terra. Opus é elegível, mas não foi chamado por proibição do pacote. Auditoria não
admite paralelismo interno nem checkpoint parcial.

---

## 9. Prompts SDD da rodada

| Ordem | Comando | Situação | Saída |
| :-- | :--- | :--- | :--- |
| 1 | `/speckit-specify` | revisado | 5 histórias, 30 FR, 9 SC |
| 2 | `/speckit-clarify` | revisado | D-P01–D-P04 pendentes e prontas ao PO |
| 3 | `/speckit-plan` | revisado | nove princípios reais, desenho e contratos condicionais |
| 4 | `/speckit-tasks` | revisado | 79 tasks em 8 fases |
| 5 | `/speckit-analyze` | revisado | 0 CRITICAL documental; achados incorporados |
| 6 | **`roteiro.md`** | aguardando aprovação | este documento, sem assinatura |
| 7 | `/speckit-implement` | bloqueado | somente depois das decisões e aprovação explícita |

### Resultado dos checklists

Requirements, technical e security estão **CONDITIONAL PASS**. D-P01–D-P04 permanecem desmarcadas;
checklist não assina portão.

### Achados incorporados

| Achado | Sev. | Decisão |
| :--- | :--- | :--- |
| Java precisa zerar | HIGH | zero 314/189, 177 nomes; 22 supressões e ratchet removidos |
| auditoria separada | HIGH | Phase 3 atômica |
| rollback Mockito | HIGH | integração proxied/PostgreSQL/checked sem transação de teste |
| identidade/correlação SDK | HIGH | D-P01, preservação recomendada |
| Keycloak incompleto | MEDIUM | D-P02, preservação recomendada |
| realm/escopo | MEDIUM | D-P03 condicional, seções derivadas apenas |
| frontend residual | MEDIUM | D-P04 humano após medição |
| paths/contagens | MEDIUM | 79 tasks nominais e números atuais |
| linguagem spec | MEDIUM | histórias/SC por contrato |

---

## 10. Riscos e mitigações

| # | Risco | Gravidade | Mitigação |
| :-- | :--- | :--- | :--- |
| 1 | misturar autores após rename/reuso | alta | D-P01; identidade estável publicada |
| 2 | perder correlação | alta | D-P01; não aceitar nome como equivalência |
| 3 | schema/writer incompatível | alta | bloco único sem checkpoint |
| 4 | falso positivo de rollback | alta | teste sem transação própria e mutação no `rollbackFor` real |
| 5 | perder ciclo/compensação Keycloak | alta | D-P02; não remover classes antes de release equivalente |
| 6 | realm apagar clients/mappers/defaults | alta | D-P03; derivar só partes e gate semântico |
| 7 | dívida Java sobreviver menor | alta | alvo zero e remoção de baseline/ratchet |
| 8 | residual frontend aceito pelo agente | média | D-P04 desmarcada e inventário nominal |
| 9 | CI/Docker ainda usarem flag | média | inventário operacional e expansão T004 |

---

## 11. Fora de escopo

| Item | Destino |
| :--- | :--- |
| editar SDK Java/Angular nesta rodada | rodada própria + publicação, se D-P01/D-P02 aprovarem |
| ADACI | roadmap próprio |
| keycloak-angular 22 / TypeScript 7 | atualização futura |
| multi-tenancy | requisito futuro |
| promover commons à SDK | roadmap OK-2 |
| alterar constituição/regras imutáveis para limpar menção textual | processo de definições, não S-codex |

---

## 12. Critérios de aprovação

### Decisões deste portão

- [ ] **D-P01**: preservar por release SDK prévia (recomendado) ou registrar emenda de perda de identidade/correlação.
- [ ] **D-P02**: preservar por release SDK prévia (recomendado) ou registrar emenda de perda no ciclo/roles Keycloak.
- [ ] **D-P03**: se aplicável, expandir ambiente/GitHub/README e limitar geração às seções derivadas.
- [ ] O escopo, atomicidade, zero Java, testes reais e execução Sol/Codex estão aprovados.

### Decisão posterior à implementação

- [ ] **D-P04**: cada residual frontend medido foi aceito ou devolvido para correção pelo product owner.

### Para concluir a rodada depois do código

- [ ] Backend `clean install` verde, Java zero, supressões/ratchet removidos.
- [ ] Frontend `npm ci`, testes, build e Prettier verdes; residual decidido por humano.
- [ ] Auditoria/Keycloak preservam contratos escolhidos e quickstart passa.
- [ ] Analyze final sem CRITICAL nem violação constitucional.

**Portão humano não assinado.** O roteiro está aguardando aprovação; pedidos anteriores de prosseguir
não resolvem D-P01–D-P04 nem autorizam `/speckit-implement`.

---

## Referência

[spec.md](spec.md) · [plan.md](plan.md) · [research.md](research.md) ·
[data-model.md](data-model.md) · [contracts/](contracts/) · [tasks.md](tasks.md) ·
[quickstart.md](quickstart.md) · [analysis.md](analysis.md) · [checklists/](checklists/)
