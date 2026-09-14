# Roteiro da Rodada — 002 Conformidade GEMS

**Feature**: `002-conformidade-gems` · **Branch**: `feature/conformidade-gems` · **Requisito**: prova S-codex da rodada 010 do Meduc (decisão 8)
**Data**: 2026-09-14 · **Status**: fase 1 concluída; planejamento não iniciado

> **O que é este documento.** A peça de aprovação da rodada. Nesta fase 1 existem somente o objetivo
> e as decisões de entrada; banco, backend, frontend, testes, impacto, tarefas, riscos, fora de escopo
> e critérios de aprovação serão consolidados na fase 2. Não há portão assinado e nenhuma
> implementação está autorizada por este estado.

---

## 1. Objetivo da rodada

Tornar o `sample-single-tenant-workspace` um molde coerente com a constituição 2.0.0, com as regras
GEMS 2.4.0 e com as SDKs publicadas Java 3.1.0 e Angular 2.1.0. A rodada conforma os mecanismos e a
forma do código, substitui duplicações locais cobertas pela SDK e reduz a dívida congelada pelos
gates, preservando o comportamento observável de parceiro, solicitação, localidade, normalização,
usuário, consulta de auditoria, autorização e upload S3.

### Por que este requisito agora

A rodada é a segunda metade da prova final da 010 do Meduc: depois da propagação das definições e da
instalação dos gates em S-def, o Codex conduz sozinho a conformidade do sample. O congelamento T057
fixou o estado real da dívida e o checkpoint T058 confirmou o reator verde com 204 testes. Planejar
agora evita usar como verdade as medições históricas do levantamento e prova se o próprio molde
consegue convergir para as regras que deve ensinar.

### O que a rodada carrega junto

A adoção dos módulos transversais da SDK exige medir o grafo antes e depois, nomear e preservar cada
garantia local, provar a equivalência neste repositório por teste e mutação e remover a implementação
local na mesma alteração. Os gates instalados em S-def também fazem parte da entrega: as constantes e
os conjuntos congelados devem encolher com o código conformado, sem transformar dívida em exceção.
Os assets de silent check-sso instalados pelo orquestrador são contribuição aprovada de S-def e serão
creditados em OK-1; o Codex produzirá o roadmap OK-2 ao final da prova.

---

## 2. Decisões de entrada

| # | Ponto | Decisão | O que isso fecha |
| :-- | :--- | :--- | :--- |
| 1 | Sequência e condução | **Executar a rodada 002 dentro da rodada 010 do Meduc, com o Codex como condutor da prova S-codex e sem delegação para Terra.** | Aplica a decisão 8 da 010, preserva a sessão T058 e evita reabrir a ordem da rodada ou terceirizar o julgamento. |
| 2 | Autoridade das definições | **Constituição 2.0.0 e regras GEMS 2.4.0 prevalecem sobre o código consolidado; `medicao-gates.md` prevalece sobre números históricos de `levantamento.md` e `MIGRACAO-2.4.0.md`.** | Fecha a hierarquia usada no planejamento e impede tratar o diagnóstico anterior à S-def como estado atual. |
| 3 | Fronteira funcional | **Preservar parceiro, solicitação, localidade, normalização, usuário, consulta de auditoria e upload S3; conformar forma e mecanismos, sem remover o conteúdo demonstrado pelo molde.** | Impede que saneamento técnico elimine features existentes ou altere escopo de negócio. |
| 4 | SDK Java | **Adotar a versão publicada Java GEMS SDK 3.1.0, declarada uma vez no POM pai, após medir o `dependency:tree` antes e depois.** | Habilita os módulos atuais sem versão em filhos e transforma o alcance da atualização em evidência. |
| 5 | SDK Angular | **Adotar `@gabriel-mdias/angular-gems-sdk` 2.1.0 com os `overrides` aprovados e retirar `--legacy-peer-deps` no mesmo bloco.** | Fecha a resolução de peers e habilita os componentes e contratos frontend atuais sem antecipar `keycloak-angular` 22. |
| 6 | Mapeamento | **Migrar os seis services medidos de `ModelMapper` para MapStruct com `GemsMappingConfig` e `unmappedTargetPolicy = ERROR`.** | Faz campo novo não mapeado reprovar na compilação e remove `ModelMapper` como padrão ensinado pelo sample. |
| 7 | Transações | **Trocar `jakarta.transaction.Transactional` por `org.springframework.transaction.annotation.Transactional`, usando `rollbackFor`, com mutação que prove rollback nas exceções lançadas pelos dois services afetados.** | Autoriza a mudança observável de semântica transacional e define sua prova de não regressão. |
| 8 | Auditoria | **Adotar `gems-auditing` e remover a implementação local na mesma alteração, preservando a tela e os contratos de consulta somente leitura, histórico do cadastro e agregados, busca ampla, campos alterados, autoria e data final inclusiva; incluir `CD_ACTOR_TYPE` por Liquibase.** | Nomeia as garantias que a adoção deve provar e impede coexistência ou perda da consulta existente. |
| 9 | Keycloak e autorização backend | **Adotar `gems-keycloak-admin` e `gems-security-authorization`, removendo as quatro classes administrativas locais e derivando o catálogo do enum com gate.** | Substitui os mecanismos duplicados, preservando autorização por ação e exigindo prova local das garantias atuais de bootstrap, perfis e integração com usuário. |
| 10 | Conformidade backend obrigatória | **Aplicar as regras vigentes a controllers, repositories, services, handlers, OpenAPI, eventos, persistência, validação, observabilidade e S3; agente não cria `GEMS-EXCEPTION` para liberar gate.** | Fecha por convenção constitucional os desvios já medidos, inclusive dois controllers sem `BaseController`, quatro repositories fora do fragmento e envelope de erro local. |
| 11 | Conformidade frontend obrigatória | **Migrar autorização de `hasRole` para `isAuthorized` por ação, usar rotas `list/form/form/:id/view/:id`, mover estruturas compartilhadas para `shared/`, substituir componentes locais cobertos pela SDK e aplicar signals, stores, formulários, testes e tokens visuais vigentes.** | Fecha os desvios frontend congelados sem criar uma nova feature ou um segundo dono para componentes transversais. |
| 12 | Catracas e baseline | **Usar o congelamento T057 como ponto de partida: 314 ocorrências Checkstyle em 22 supressões, 189 identidades de arquitetura/fonte e 174 ocorrências frontend; Spotless ratchet em `411156d`; zero exceções escritas. Cada bloco reduz também a constante e o conjunto congelado.** | Impede crescimento, supressão genérica e falso verde por redução apenas do número agregado. |
| 13 | Gate de referência | **Tomar como baseline executável o T058: `BUILD SUCCESS`, 204 testes — 13 security, 132 core e 59 deploy — sem falhas ou erros.** | Fixa a contagem real anterior à implementação para a futura prova de não regressão. |
| 14 | Dependências já decididas | **As decisões D-01/D-02 e as quatro labels criadas não serão reabertas nesta rodada; o alinhamento restante segue os destinos já registrados.** | Evita misturar manutenção Dependabot já decidida com a conformidade do sample. |
| 15 | Single tenant | **`multi-tenant.md` é referência para extensão futura; esta rodada mantém o sample single tenant.** | Impede importar schema por tenant ou mecanismos exclusivos do Meduc. |
| 16 | Fora da fronteira | **ADACI não é alvo; `keycloak-angular` 22, TypeScript 7 e promoção dos utilitários `commons/` para a SDK permanecem nos destinos já registrados.** | Evita expansão silenciosa da prova S-codex e preserva as decisões de evolução futura. |
| 17 | Evidência da prova 010 | **Creditar os assets silent check-sso de S-def ao orquestrador em OK-1 e produzir o roadmap OK-2 pelo Codex no fechamento.** | Mantém autoria e limites verificáveis entre preparação S-def e execução S-codex. |
