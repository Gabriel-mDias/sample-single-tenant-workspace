# Change: consumir Java GEMS SDK 3.3.1

**Data**: 2026-09-25 · **Tipo**: correção de sigilo

## Intenção

A 3.3.0 consumida pelo Sample ainda registrava caminho, corpo ou causa de exceções nos logs dos handlers. A correção da PR Java GEMS SDK #37 entrou na `main` e foi publicada em `v3.3.1`. Fixar a versão corretiva permite verificar o comportamento no consumidor real.

## Fronteira

- **EDIT**: versão central da Java SDK em `backend/pom.xml` e regressão de integração dos handlers em `ErrorEnvelopeIntegrationTest`.
- **Fora de escopo**: contrato do envelope HTTP, configurações do realm e frontend.

## Prova e risco

O teste envia uma sentinela como segmento de rota e causa de erros 500/502. Os status permanecem 500/502 e o log capturado não contém a sentinela. O reator com PostgreSQL 16/Liquibase passou com 225 testes; o check estrito do toolkit passou com 37 regras. A mudança de dependência ainda requer CI no SHA da PR. Reverter a versão retorna à política de log da 3.3.0.

Esta correção fecha a lacuna conhecida dos handlers da SDK. O aceite global de P06 continua condicionado aos 12 invariantes, 11 notas justificadas e zero crítico aplicável demonstrado.
