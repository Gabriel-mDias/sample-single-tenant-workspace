# P06 — retomada em andamento (2026-09-24)

Este registro acompanha a PR draft #35. Estado no momento do commit: candidato remoto validado `13e48fd5d659ced39e77bb465615dd62cdfa75c0`, CI 3/3 aprovado (run 36084477209). A alteração `PageResponseDTO` descrita em `change-p06-page-response-contract.md` será incluída como trabalho em andamento e não está validada. Não usar este commit/tag como aceite da P06.

## O que já foi concluído e tem evidência

- SDK Angular corrigida foi integrada/publicada em 2.3.2 pelas PRs #16 e #17; o Sample declara e trava versão exata do registry. Houve autorização específica para essas integrações/publicações.
- As rotas protegidas e a documentação da constituição foram alinhadas ao contrato `data.authorization`; a navegação autenticada confirmou o comportamento.
- Lighthouse autenticado nas rotas home, novo parceiro e nova solicitação passou 100/100/100 com SDK 2.3.2; os seletores `aria-command-name`, `color-contrast` e `label` não reportaram falhas. Menu e calendário foram exercitados por Enter e Espaço, com foco preservado.
- Baseline → mutação reprovada → restauração aprovada está demonstrado para gates frontend, toolkit e fronteira backend. A regra JavaParser de escrita direta em repository de outro domínio foi testada por mutação.
- No SHA `13e48fd5d659ced39e77bb465615dd62cdfa75c0`: 126 testes Angular, build frontend, 215 testes Java, reator Maven, gates/scanners e CI 3/3 aprovados. Consultar a pasta de evidências externa `D:\repos\tasks\proposta-solucao-pendencias\evidencias\` para logs, JSONs, hashes e procedimento completos: `P06-validacao.md`, `P06-browser-a11y.md`, `P06-sdk-232-final-a11y.md`, `P06-fronteiras-inventario.md`, `P06-assinatura-inventario.md` e `P06-matriz-sample.md`.

## Bug novo observado — serialização de página

Ao encerrar os serviços locais, o Spring Data emitiu: `Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability of the resulting JSON structure!`. Os controllers de parceiro, solicitação e auditoria retornavam `Page` diretamente. O frontend usa os campos `content`, `totalElements`, `totalPages`, `number` e `size` do tipo `PageResponse<T>` em `frontend/src/app/core/models/page-response.ts` (confirmar caminho exato se o layout mudar). Foi iniciada uma resposta DTO explícita para preservar esses campos; falta teste de serialização e validação do candidato.

## Assinatura P06 ainda aberta

A matriz tem 12 invariantes obrigatórios e 11 pesos. Provas parciais existem, mas os invariantes não estão integralmente demonstrados e a rubrica segue sem notas justificadas. Lacunas do inventário:

1. Direção de dependências: escrita direta interdomínio coberta por AST/mutação; revisar escrita indireta e queries mutáveis customizadas.
2. Contratos: builds passam; comparar todos os campos, códigos/status e formatos REST com contratos e usos frontend. O risco `PageImpl` está registrado acima.
3. Validações: ampliar limites, filtros, CEP e transições além dos caminhos já cobertos.
4. Erros: percorrer handlers e saídas de negócio, 401/500 e ausência de dados sensíveis em todas as respostas.
5. Autorização: comparar matriz completa de 21 ações, perfis, endpoints, campos e controles frontend/backend.
6. Transação/persistência: inventariar cada escrita, migrations históricas e boot em ambiente publicado.
7. Auditoria/observabilidade: inspecionar logs, métricas, traces e respostas quanto a dados pessoais/segredos.
8. Frontend: expandir rotas, temas e estados de interação além das três telas/estados Lighthouse já aprovados.
9. Testes: inventariar redes de teste e mutações representativas para todos os contratos.
10. Rastreabilidade: completar critério de história → tarefa → código → teste/inspeção → resultado → SHA.
11. Baseline/waivers: reconciliar desvios e exceções integralmente; não criar waiver para elevar a nota.
12. Gates/SHA: mapear cada obrigação e zero crítico a provas suficientes no SHA final.

Condições de aceite permanecem: sequência baseline → mutação reprovada → restauração aprovada, todos os gates e CI obrigatórios no SHA candidato, Lighthouse sem achados impeditivos, 12/12 invariantes, pelo menos 90/100 com justificativas e zero crítico demonstrado. Até cumprir tudo, manter PR #35 draft, P06 `in_progress` e P07 pendente. Merge do Sample é fora de P06; revisão formal das PRs está em P15.

## Próximas ações

1. Testar serialização da nova resposta paginada, incluindo nomes/valores dos cinco campos e ausência de metadados instáveis de `PageImpl`.
2. Executar bundle backend e gate focalizado; depois completar análise de impacto e repetir frontend/backend, build, scanners, CI e verificações autenticadas que o diff afetar.
3. Atualizar inventário/matriz/PR no novo SHA somente após gates; manter a PR como draft.
4. Fechar individualmente as 12 lacunas e pontuar os 11 critérios com referências verificáveis. Repetir gates afetados para cada novo SHA.
5. Marcar P06 passed e liberar P07 apenas após evidência completa dos critérios acima.

Ao retomar, leia este documento, `change-p06-page-response-contract.md`, os arquivos de evidência externa indicados, confira `git status` e a PR #35 antes de agir. O commit/tag `[EM ANDAMENTO]` é apenas checkpoint de persistência.
