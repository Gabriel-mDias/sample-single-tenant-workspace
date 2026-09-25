# Change: exercitar a matriz de autorização dos endpoints próprios

**Data**: 2026-09-25 · **Tipo**: teste de integração

## Intenção

O gate existente confirma que os métodos dos controllers declaram `@PreAuthorize`, mas a prova HTTP por perfil só exercitava cenários amostrais. A nova matriz executa os 19 endpoints próprios com os perfis administrador, operador e consulta, além de token sem ações e ausência de token. As permissões esperadas são declaradas independentemente das anotações, a partir do contrato de perfis.

## Prova e limite

`AutorizacaoMatrizHttpIntegrationTest` usa MockMvc/JWT e PostgreSQL 16/Liquibase. Para perfis proibidos exige 403; para perfis permitidos exige que a requisição atravesse a autorização. IDs inexistentes e corpos inválidos evitam escritas acidentais; erros de negócio posteriores à autorização são aceitos nesse teste. O teste focal passou com 19 casos parametrizados.

A matriz cobre a decisão de acesso HTTP por perfil e endpoint. Não substitui testes de sucesso dos contratos de resposta, estado de transição, campo de comissão, ausência de efeitos laterais nos casos proibidos nem o aceite global de P06.
