# Registro de mudança P06 — contrato paginado estável

**Estado:** em validação. **Branch/PR:** `task/p06-sample-frontend`, draft #35. **Base do contrato:** `8d75c22a944f1eac58a95db496370ebb8c3c7a09`.

## Motivo e evidência

Na execução local do Sample, o Spring Data avisou que serializar `PageImpl` diretamente não garante estabilidade do formato JSON. A API retorna páginas nos controllers de parceiro, solicitação e auditoria. O consumidor Angular modela `PageResponse<T>` com `content`, `totalElements`, `totalPages`, `number` e `size`. Mudanças acidentais nos detalhes de serialização do framework podem quebrar silenciosamente esse contrato.

## Alteração proposta presente neste checkpoint

Adicionar `PageResponseDTO<T>` que materializa explicitamente esses cinco campos de `Page<T>` e adaptar os três controllers paginados para responder com esse DTO. A intenção é manter os campos já usados pelo frontend e impedir a exposição da estrutura interna de `PageImpl`.

Arquivos afetados neste checkpoint:

- `backend/app-core/src/main/java/br/com/gems/app/commons/response/PageResponseDTO.java` (novo)
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/controller/AuditoriaController.java`
- `backend/app-core/src/test/java/br/com/gems/app/commons/response/PageResponseDTOTest.java` (regressão de JSON)
- `backend/app-core/src/test/java/br/com/gems/app/commons/response/BuscaPaginadaControllerTest.java` (quatro rotas HTTP)
- `specs/001-parceiro-e-solicitacao/contracts/rest-api.md` (contrato público das buscas)

## Validação obrigatória antes de considerar pronto

- Teste de serialização com página 2, tamanho 10 e 21 elementos: `PageResponseDTOTest` confere os cinco nomes e valores e reprova qualquer campo adicional de `PageImpl`. Passou no teste focalizado de 25/09/2026 (exit 0).
- `BuscaPaginadaControllerTest` exercita via MockMvc as buscas de parceiro, solicitação e auditoria e o histórico de auditoria com página 2/tamanho 10; exige os cinco campos e a ausência de `pageable`. Passou no teste focalizado de 25/09/2026 (exit 0).
- Executar bundle/gates do backend, testes focados e `mvnw clean install`; analisar impacto sobre consumidores e exercitar endpoints paginados no frontend.
- Repetir frontend build/test, gates/scanners e CI conforme análise de impacto; repetir Lighthouse autenticado se houver impacto em telas auditadas.
- Registrar procedimento, esperado, observado, exit codes, resultado e SHA atualizado em evidências/matriz/PR.

## Limite deste registro

Os testes focalizados confirmam o JSON do DTO e o roteamento/JSON HTTP com serviços simulados. A execução autenticada com banco real, o CI no novo SHA e a matriz 12/12 ainda precisam de prova. A PR #35 permanece draft e P06 aberta.
