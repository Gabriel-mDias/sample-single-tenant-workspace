# Implementation Plan: Conformidade GEMS

**Branch**: `feature/conformidade-gems` | **Date**: 2026-09-14 | **Spec**: [spec.md](spec.md)

## Summary

Zerar a dívida Java, levar o frontend ao mínimo medido e adotar somente módulos SDK cujas garantias
publicadas cubram o comportamento atual. Auditoria e Keycloak ficam bloqueados até D-P01/D-P02; a
recomendação é preservar comportamento por evolução/publicação prévia das SDKs.

## Technical Context

**Language/Version**: Java 21; TypeScript 6.0; Angular 22.1
**Dependencies**: Spring Boot 4.1.1; GEMS Java 3.1.0 atual; Angular GEMS 2.1.0; Keycloak 26
**Storage**: PostgreSQL 16; Liquibase; schemas `public`, `security`, `auditoria`
**Testing**: JUnit 5, Spring/Testcontainers, Mockito para colaboração, ArchUnit, Vitest, scripts Node
**Platform**: JVM/Linux e browser; Docker Compose/Keycloak config CLI
**Constraints**: single tenant; zero dívida Java; frontend residual só com aceite humano; nenhuma API presumida
**Baseline**: 204 testes backend; 21 arquivos/118 testes frontend; 314/22, 189 e 174 ocorrências

## Constitution Check

### Gate antes do desenho

| Princípio constitucional real | Estado | Avaliação |
| :--- | :--- | :--- |
| I. Fronteiras de Módulo Impostas pelo Reator Maven | PASS | SDK entra no módulo consumidor; grafo `security/model/events → core → deploy` não ganha ciclo. |
| II. Persistência Declarada, Nunca Inferida | **PASS DE PORTÃO; BLOQUEADO POR RELEASE** | D-P01 preserva identidade/correlação; o bloco espera SDK compatível publicada. |
| III. Changelog Liquibase Organizado e Rastreável | PASS DE DESENHO | Ordem, preconditions, backfill e teste de upgrade estão exigidos; execução virá na fase 3. |
| IV. Validação Acumulada e Ordem Canônica de Serviço | PASS DE DESENHO | Seis services, validação e ordem serão corrigidos; gates Java devem zerar. |
| V. Contrato REST e Autorização Espelhada na UI | **PASS DE PORTÃO; BLOQUEADO POR RELEASE** | D-P02 preserva ciclo/bootstrap; o bloco espera SDK compatível publicada. |
| VI. Frontend Modular e Padronizado pela GEMS SDK | **PASS DE DESENHO** | Correções estão definidas; D-P04 continua no portão posterior à medição. |
| VII. Testes como Gate (NÃO NEGOCIÁVEL) | PASS DE DESENHO | Testes primeiro, mutações e rollbacks integrados sem transação de teste estão especificados. |
| VIII. Especificação Antes de Código, Sem Sobras de Exemplo | PASS | Fase 2 documental concluída e portão assinado antes do código. |
| IX. Configuração de Ambiente é Código | PASS | D-P03 não se aplica; ambiente/realm preservados. T004 autoriza somente os três arquivos operacionais nominais. |

### Gate de adoção de módulos

| Módulo | Publicado | Alcance medido | Garantia equivalente | Estado |
| :--- | :--- | :--- | :--- | :--- |
| `gems-mapstruct` | sim, 3.1.0 | a medir antes/depois | sim | apto após teste local |
| `gems-security-authorization` | sim, 3.1.0 | a medir | sim para catálogo/scan | apto após teste local |
| `gems-auditing` | sim, 3.1.0 | fonte aferida | **não** para id/correlação | bloqueado D-P01 |
| `gems-keycloak-admin` | sim, 3.1.0 | fonte aferida | **não** para ciclo/roles | bloqueado D-P02 |
| Angular GEMS | sim, 2.1.0 | peers aferidos | a provar por componente | apto condicional |

O portão não supera os bloqueios materiais: `gems-auditing` e `gems-keycloak-admin` só entram após
releases compatíveis publicadas e aferidas. D-P04 continua no portão posterior à medição.

## Project Structure

Mantém os cinco módulos Maven, `frontend/src/app/{core,shared,features}` e os ambientes atuais. Novos
testes/mappers entram no módulo nominal indicado em `tasks.md`; nenhuma nova camada ou SDK local.

## Sequência e atomicidade

1. Decisões humanas e releases prévias exigidas.
2. Dependências e árvores antes/depois.
3. Autorização/catalogação e Keycloak somente quando D-P02 liberar.
4. Auditoria em um bloco único somente quando D-P01 liberar.
5. Mappers/transações e conformidade Java até zero.
6. Plataforma e features Angular; medir residual e submeter D-P04.
7. Remover supressões/baselines/ratchet; gates completos e analyze final.

Auditoria não possui checkpoint intermediário publicável. O commit do orquestrador só pode ocorrer
depois de migration + reader + provider + writer SDK + remoções locais + testes verdes.

## Complexity Tracking

Nenhuma violação foi justificada. D-P01/D-P02 preservam garantias, D-P03 não se aplica e D-P04 é uma
decisão posterior prevista, não exceção.
