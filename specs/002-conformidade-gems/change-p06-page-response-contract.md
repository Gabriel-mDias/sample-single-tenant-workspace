# Registro de mudança P06 — contrato paginado estável

**Estado:** em andamento, sem validação. **Branch/PR:** `task/p06-sample-frontend`, draft #35. **Base validada anterior:** `13e48fd5d659ced39e77bb465615dd62cdfa75c0`.

## Motivo e evidência

Na execução local do Sample, o Spring Data avisou que serializar `PageImpl` diretamente não garante estabilidade do formato JSON. A API retorna páginas nos controllers de parceiro, solicitação e auditoria. O consumidor Angular modela `PageResponse<T>` com `content`, `totalElements`, `totalPages`, `number` e `size`. Mudanças acidentais nos detalhes de serialização do framework podem quebrar silenciosamente esse contrato.

## Alteração proposta presente neste checkpoint

Adicionar `PageResponseDTO<T>` que materializa explicitamente esses cinco campos de `Page<T>` e adaptar os três controllers paginados para responder com esse DTO. A intenção é manter os campos já usados pelo frontend e impedir a exposição da estrutura interna de `PageImpl`.

Arquivos afetados neste checkpoint:

- `backend/app-core/src/main/java/br/com/gems/app/commons/response/PageResponseDTO.java` (novo)
- `backend/app-core/src/main/java/br/com/gems/app/parceiro/controller/ParceiroController.java`
- `backend/app-core/src/main/java/br/com/gems/app/solicitacao/controller/SolicitacaoController.java`
- `backend/app-core/src/main/java/br/com/gems/app/auditoria/controller/AuditoriaController.java`

## Validação obrigatória antes de considerar pronto

- Criar teste de serialização com uma página não trivial (página 2, tamanho 10, 21 elementos): confirmar `content`, `totalElements=21`, `totalPages=3`, `number=2`, `size=10`; confirmar que a resposta expõe exatamente os nomes de contrato necessários e não campos internos de `PageImpl`.
- Executar bundle/gates do backend, testes focados e `mvnw clean install`; analisar impacto sobre consumidores e exercitar endpoints paginados no frontend.
- Repetir frontend build/test, gates/scanners e CI conforme análise de impacto; repetir Lighthouse autenticado se houver impacto em telas auditadas.
- Registrar procedimento, esperado, observado, exit codes, resultado e SHA atualizado em evidências/matriz/PR.

## Limite deste registro

No momento do checkpoint não existe teste de regressão para esta classe, os testes/build ainda não foram executados sobre o diff, não há commit validado nem CI deste conteúdo. A PR #35 deve permanecer draft e P06 aberta. Descartar ou corrigir a implementação se qualquer gate ou contrato não passar; não declarar a estabilidade apenas pela forma do DTO.
