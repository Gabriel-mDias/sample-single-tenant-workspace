---
name: spec-driven
description: Execute o ciclo Spec-driven do projeto para novas funcionalidades, novos CRUDs, mudanÃ§as de comportamento e alteraÃ§Ãµes em especificaÃ§Ãµes aprovadas. Use quando o Codex precisar criar ou atualizar spec.md, esclarecer requisitos, produzir plan/research/data-model/contracts/quickstart, gerar ou analisar tasks/checklists, ou implementar uma feature governada pelo spec-kit. NÃ£o use para bug fix que apenas restaura comportamento jÃ¡ contratado nem para ediÃ§Ã£o exclusivamente documental.
---

# Spec-driven do projeto

## Preparar o contexto

1. Ler `AGENTS.md`, `specs/AGENTS.md` e os artefatos da feature. Ler `CODEX.md` e a constituiÃ§Ã£o por
   inteiro somente quando a mudanÃ§a tocar comportamento, arquitetura, ambiente ou governanÃ§a.
2. Localizar a RF de origem em `utils/documentaÃ§Ã£o do projeto/especificacoes_spec_driven/`.
3. Consultar o levantamento prÃ©vio apenas para contexto de negÃ³cio e terminologia.
4. Inspecionar a feature semelhante e o domÃ­nio `parceiro` para restriÃ§Ãµes reais de implementaÃ§Ã£o.
5. Classificar o trabalho e aplicar documentaÃ§Ã£o proporcional:
   - Nova feature: executar o ciclo completo.
   - MudanÃ§a de comportamento aprovado: atualizar a partir do primeiro artefato impactado e propagar
     para todos os seguintes.
   - Bug que restaura comportamento jÃ¡ especificado: sair deste workflow, criar teste de regressÃ£o e
     seguir a rota de bug do `CODEX.md`.
   - RefatoraÃ§Ã£o ou ajuste tÃ©cnico sem mudanÃ§a de comportamento: criar `change.md` a partir de
     `.specify/templates/change-template.md`; nÃ£o criar `research`, `data-model` ou contratos vazios.

NÃ£o implementar diretamente a partir de RF preliminar. NÃ£o tratar `[NEEDS CLARIFICATION]` como
decisÃ£o. NÃ£o permitir que detalhes do cÃ³digo contaminem o `spec.md`; usar o cÃ³digo para restriÃ§Ãµes e
estado real durante o planejamento.

## Executar o ciclo

### 1. Specify

- Criar ou atualizar `specs/<numero>-<slug>/spec.md` usando
  `.specify/templates/spec-template.md` como estrutura.
- Descrever valor, atores, user stories independentes, cenÃ¡rios, requisitos funcionais, edge cases,
  premissas, fora de escopo e critÃ©rios mensurÃ¡veis.
- Manter o spec tecnolÃ³gico-agnÃ³stico: definir **o quÃª** e **por quÃª**, nÃ£o classes, tabelas ou
  frameworks.
- Criar ou atualizar `checklists/requirements.md` e corrigir falhas de completude antes de avanÃ§ar.

### 2. Clarify

- Encontrar ambiguidades de escopo, autorizaÃ§Ã£o, dados, UX, concorrÃªncia, integraÃ§Ã£o e falhas.
- Resolver primeiro decisÃµes com alto impacto e sem default seguro.
- Registrar cada resposta no `spec.md`, eliminando todos os marcadores
  `[NEEDS CLARIFICATION]` antes do plano.
- NÃ£o inventar decisÃ£o de produto quando alternativas alterarem materialmente o resultado.

### 3. Plan

- Usar `.specify/scripts/powershell/setup-plan.ps1 -Json` quando for inicializar os artefatos de
  planejamento de uma feature.
- Produzir e manter os artefatos aplicÃ¡veis; nÃ£o criar arquivo vazio apenas para cumprir lista:
  - `plan.md`: contexto tÃ©cnico, constitution check, estrutura e justificativas de complexidade.
  - `research.md`: decisÃµes, riscos, alternativas rejeitadas e evidÃªncias.
  - `data-model.md`: entidades, relacionamentos, validaÃ§Ãµes, Ã­ndices e transiÃ§Ãµes de estado.
  - `contracts/`: REST e mÃ³dulo frontend, quando aplicÃ¡veis.
  - `quickstart.md`: cenÃ¡rios executÃ¡veis de validaÃ§Ã£o ponta a ponta.
- Distribuir arquivos entre os cinco mÃ³dulos Maven conforme a constituiÃ§Ã£o.
- Manter configuraÃ§Ã£o, migrations, resources e empacotamento somente em `app-deploy`.
- Consultar regras e recipes GEMS apenas para as camadas presentes no plano; aplicar os overrides do
  `CODEX.md` e preferir `parceiro` quando houver divergÃªncia consolidada.

### 4. Tasks

- Usar `.specify/scripts/powershell/setup-tasks.ps1 -Json` quando for inicializar `tasks.md`.
- Organizar tarefas por dependÃªncia e user story, com IDs `T001`, `T002` etc.
- Informar caminho exato e mÃ³dulo Maven em cada tarefa.
- Usar `[P]` somente quando tarefas atuarem em arquivos diferentes e nÃ£o dependerem de trabalho
  incompleto.
- Colocar teste antes da implementaÃ§Ã£o para regra de negÃ³cio e regressÃµes.
- Incluir checkpoints executÃ¡veis apÃ³s cada fase significativa.
- Registrar a decisÃ£o de delegaÃ§Ã£o. `Sem delegaÃ§Ã£o` Ã© vÃ¡lida quando o onboarding nÃ£o se amortiza;
  modelo caro exige justificativa de risco material.

### 5. Checklists e anÃ¡lise

- Criar checklists especÃ­ficos para riscos materiais: requirements, tÃ©cnico, UX, seguranÃ§a,
  acessibilidade ou integraÃ§Ã£o.
- Tratar checklist como teste da qualidade dos requisitos, nÃ£o como teste da implementaÃ§Ã£o.
- Analisar consistÃªncia e rastreabilidade entre constituiÃ§Ã£o, spec, plan, research, data model,
  contracts, quickstart e tasks.
- Classificar como crÃ­tico qualquer violaÃ§Ã£o constitucional, artefato obrigatÃ³rio ausente, contrato
  contraditÃ³rio ou requisito essencial sem cobertura.
- NÃ£o marcar item aberto mecanicamente. Resolver, aceitar explicitamente ou registrar por que nÃ£o se
  aplica.
- NÃ£o alterar arquivos durante uma execuÃ§Ã£o solicitada como anÃ¡lise read-only.

### 6. Implement

- Confirmar que nÃ£o restam bloqueios crÃ­ticos nem clarificaÃ§Ãµes materiais.
- Preservar o escopo e executar `tasks.md` na ordem de dependÃªncia.
- Para executor delegado, exigir pacote com listas fechadas `READ`, `EDIT`, `CREATE` e `TEST`; se a
  leitura for insuficiente, o executor para e reporta em vez de explorar o repositÃ³rio.
- Escrever ou ajustar testes antes da lÃ³gica de negÃ³cio correspondente.
- Manter `tasks.md` sincronizado com o trabalho realmente concluÃ­do.
- NÃ£o refatorar Ã¡reas nÃ£o relacionadas sÃ³ porque estÃ£o prÃ³ximas da feature.
- Atualizar documentaÃ§Ã£o de agente quando surgir uma convenÃ§Ã£o transversal ou um override recorrente.

### 7. Verify

- Executar os gates aplicÃ¡veis definidos no `CODEX.md` e na constituiÃ§Ã£o.
- Usar gate focado durante a iteraÃ§Ã£o e executar o gate completo uma vez no fechamento. Para entrega
  completa, exigir:
  - `backend/.\mvnw.cmd clean install`.
  - `frontend/npm test`.
  - `frontend/npm run build`.
  - `frontend/npx prettier --write .`.
  - aplicaÃ§Ã£o subindo com `ddl-auto: validate` quando houver impacto de contexto ou banco.
- Executar os cenÃ¡rios relevantes de `quickstart.md` e registrar evidÃªncias.
- Informar testes nÃ£o executados e verificaÃ§Ãµes manuais pendentes.

## Resolver conflitos de fontes

- Aplicar a constituiÃ§Ã£o acima de qualquer outro arquivo.
- Tratar a spec-kit aprovada como contrato do comportamento pretendido.
- Tratar cÃ³digo e testes como estado real e referÃªncia de implementaÃ§Ã£o.
- Se spec aprovada e cÃ³digo divergirem, investigar bug ou drift documental; nÃ£o escolher
  silenciosamente.
- Fazer o cÃ³digo consolidado vencer apenas as especificaÃ§Ãµes preliminares de `utils/`, registrando a
  diferenÃ§a na spec convertida.
- Fazer `parceiro` vencer templates/recipes GEMS nas divergÃªncias jÃ¡ consolidadas.
- Distinguir descriÃ§Ã£o histÃ³rica de `plan.md`/`research.md` do estado atual em `tasks.md`, cÃ³digo e
  testes.

## Encerrar

Entregar um resumo com:

- artefatos criados ou atualizados;
- requisitos e user stories cobertos;
- decisÃµes e divergÃªncias registradas;
- tarefas concluÃ­das e pendentes;
- comandos de validaÃ§Ã£o e resultados;
- riscos, clarificaÃ§Ãµes ou verificaÃ§Ãµes manuais restantes.
