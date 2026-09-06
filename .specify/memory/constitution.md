<!--
SYNC IMPACT REPORT — v1.0.0 (criação)
=====================================
Version change: — → 1.0.0
Bump rationale: ratificação inicial. Nove princípios definidos, nenhum herdado de versão anterior.

Motivação: este repositório é um molde. A constituição nasce completa porque o custo de descobrir
estas regras uma segunda vez já foi pago — cada uma delas existe para fechar uma falha silenciosa
concreta: schema que diverge do código sem erro, endpoint que esquece a anotação de autorização,
rota de UI liberada por omissão do guard, alteração de ambiente feita no console que sobrevive a
todos os deploys, backup que nunca foi restaurado, e domínio de exemplo que apodrece no repositório.

Princípios definidos (9):
  I.    Fronteiras de Módulo Impostas pelo Reator Maven
  II.   Persistência Declarada, Nunca Inferida
  III.  Changelog Liquibase Organizado e Rastreável
  IV.   Validação Acumulada e Ordem Canônica de Serviço
  V.    Contrato REST e Autorização Espelhada na UI
  VI.   Frontend Modular e Padronizado pela GEMS SDK
  VII.  Testes como Gate (NÃO NEGOCIÁVEL)
  VIII. Especificação Antes de Código, Sem Sobras de Exemplo
  IX.   Configuração de Ambiente é Código

Seções adicionadas: Stack e Restrições Técnicas; Fluxo de Trabalho e Portões de Qualidade;
Governance.

Artefatos dependentes, todos criados na mesma ratificação:
  ✅ .specify/templates/{spec,plan,tasks,checklist,roteiro,change,pacote-de-execucao,estado-lote}-template.md
  ✅ AGENTS.md (raiz) e os aninhados de backend/, frontend/, environments/, specs/
  ✅ CLAUDE.md, CODEX.md, GEMINI.md, .cursor/rules/, .github/instructions/, kilo.json
  ✅ .gems-ai/rules/ e .gems-ai/recipes/

Pendências: nenhuma.

NOTA AO PROJETO QUE HERDA ESTE MOLDE — o domínio `parceiro`/`solicitacao` presente no código é
scaffold de referência e está sujeito à última cláusula do Princípio VIII: deve ser removido por
inteiro assim que o primeiro domínio real ocupar o papel de referência. Até lá, ele é a resposta
concreta a "como se faz aqui".
-->

# Constituição do projeto

Este é um sistema interno **single-tenant**, com um **segundo executável** (portal externo, app de
campo, integração) previsto no futuro. Essa previsão não é um detalhe de roadmap: ela condiciona a
arquitetura descrita no Princípio I. Se ela não valer para o seu projeto, o Princípio I é a primeira
emenda a fazer — e é uma emenda, com Sync Impact Report e bump, não uma edição.

## Core Principles

### I. Fronteiras de Módulo Impostas pelo Reator Maven

Maven fatia o backend por **responsabilidade técnica** em cinco módulos, organizados num reator com
grafo de dependência estritamente unidirecional.

| Módulo Maven | Depende de | Conteúdo | Organização de pacotes |
| :--- | :--- | :--- | :--- |
| `app-model` | — | entidades JPA, DTOs, params de filtro, responses, enums | **camada-primeiro**: `br.com.gems.app.model.entity.<agrupamento>`, `model.dto`, `model.params`, `model.response` |
| `app-security` | — | resource server Keycloak, ações, Admin API, `@PublicEndpoint` | `br.com.gems.app.security.*` |
| `app-events` | model | **somente POJOs de evento** (ver regra abaixo) | `br.com.gems.app.<dominio>.event` |
| `app-core` | model, security, events | regra de negócio | **domínio-primeiro**: `br.com.gems.app.<dominio>.{controller,service,repository}` |
| `app-deploy` | core | `AppApplication`, `application.yml`, changelogs Liquibase, fat jar | raiz |

Regras não negociáveis:

- A direção das dependências é imposta pelo **grafo do reator Maven em tempo de compilação**:
  `app-model` e `app-security` não dependem de ninguém, `app-events` depende só de `model`,
  `app-core` depende de `model`/`security`/`events`, e `app-deploy` depende de `core`. Um import na
  direção errada (por exemplo, `app-model` importando um tipo de `app-core`) **não compila**. Essa é
  a garantia de fronteira do projeto — e é mais forte do que qualquer verificação em tempo de teste,
  que só reprova depois de o código existir.
- Tudo que é **ambiente** — configuração, migração, empacotamento — mora exclusivamente em
  `app-deploy`. Um `application.yml` ou changeset que vaze para `core`/`model` inviabiliza
  reaproveitar o `app-core` no segundo executável. Esta é a razão de o módulo existir.
- **`app-events` contém somente os POJOs de evento.** Um listener que precise chamar service de
  negócio **não** pode morar ali: ele dependeria de `app-core`, e o reator recusaria o ciclo
  `events → core`. Esses listeners nascem em `app-core`, junto do domínio que reage
  (`br.com.gems.app.<dominio>.listener`).
- Comunicação entre domínios de negócio dentro de `app-core` distingue **acionar** de **consultar**:
  - **Acionar regra de negócio de outro domínio é por evento** (`ApplicationEventPublisher` +
    `@EventListener`/`@TransactionalEventListener` do próprio Spring), nunca chamada direta de
    service a service. É assim que concluir uma solicitação atualiza o parceiro sem que
    `solicitacao` conheça o service de `parceiro`.
  - **Consultar dado de outro domínio pelo repository dele é permitido.** São leituras síncronas, e
    um evento não responde pergunta — ele avisa que algo aconteceu. Forçá-las a passar por evento
    tornaria a resposta assíncrona sem ganho de isolamento nenhum.
  - O que continua proibido é **escrever** no dado de outro domínio, direta ou indiretamente:
    chamar o service dele, ou salvar pelo repository dele.
- Esta regra é **convenção revisada em code review** — não há gate automatizado que a verifique.
- **Nota sobre entrega de eventos em memória**: não há registro persistido de publicações, então
  eventos de domínio são entregues **apenas em memória**, dentro do próprio processo. Se a aplicação
  cair entre a publicação e a execução do listener, o evento se perde — não há reprocessamento.
  Isso é aceitável enquanto nenhum evento carregar consequência que não possa ser recalculada;
  quando um evento não puder ser perdido, a persistência de publicações MUST voltar como decisão
  consciente e documentada, não por omissão.
- `spring-boot-maven-plugin` só pode estar ativo em `app-deploy`; nos demais módulos fica em
  `<pluginManagement>`. Ativo no POM pai, o goal `repackage` transformaria os jars de biblioteca em
  fat jars com `BOOT-INF/`, inutilizáveis como dependência.

### II. Persistência Declarada, Nunca Inferida

`ddl-auto: validate`. O schema muda **exclusivamente** por changeset Liquibase. Hibernate nunca cria
nem altera tabela.

- **Nomenclatura física:** tabelas e colunas em MAIÚSCULO, sem prefixo de tabela. Prefixos de coluna
  obrigatórios: `ID_` (chave primária e estrangeira), `NM_` (nome, sigla), `DS_` (descrição e textos
  livres), `CD_` (código, e-mail, CEP), `NR_` (número, telefone), `VL_` (valor monetário), `DT_`
  (data e timestamp), `FL_` (booleano).
- Chave primária UUID com `@GeneratedValue(strategy = GenerationType.UUID)`.
- Entidades de negócio MUST ter **soft delete**:
  `@SQLDelete(sql = "UPDATE TABELA SET DT_EXCLUSAO = NOW() WHERE ID_X = ?")` +
  `@SQLRestriction("DT_EXCLUSAO IS NULL")`, herdando `DT_CRIACAO` e `DT_EXCLUSAO` de
  `EntidadeAuditavel` (`br.com.gems.app.model.entity.base`). Tabelas puramente referenciais,
  carregadas por seed e nunca editadas pelo usuário, são isentas e não herdam `EntidadeAuditavel`.
- Toda entidade com `@SQLDelete` MUST estender `EntidadeAuditavel`. `@SQLDelete` apontando para uma
  coluna não mapeada é **defeito**, não estilo: o delete roda, não encontra a coluna e falha em
  runtime, não no build.
- Relacionamentos `ManyToOne` são sempre `LAZY`. Com `open-in-view: false`, todo relacionamento
  consumido fora da transação MUST estar coberto por `@NamedEntityGraph` ou carregado dentro do
  service — caso contrário estoura `LazyInitializationException` em produção, não no teste.
- **Schemas:** `public` para o domínio de negócio; `security` para entidades de segurança
  persistidas; `auditoria` para logs de auditoria, disparo de e-mail e execução de schedules.
  Nenhuma outra segmentação.
- Lombok padrão nas entidades: `@Builder @AllArgsConstructor @NoArgsConstructor @Getter @Setter
  @ToString`, com `@ToString.Exclude` nos `ManyToOne` para evitar recursão.

### III. Changelog Liquibase Organizado e Rastreável

- `changelog-master.xml` é apenas um **índice**: contém a property `uuid_function`, os changesets de
  infraestrutura e uma sequência de `<include>` apontando para um arquivo por domínio, em
  `backend/app-deploy/src/main/resources/db/changelog/`.
- Um arquivo por domínio ou agrupamento (`changelog-localidades.xml`, `changelog-normalizacao.xml`,
  `changelog-parceiro.xml`). É **proibido** continuar inflando o master com `createTable`.
- `id` de changeset descritivo e estável (`create-table-parceiro`, `seed-pais`,
  `seed-estado-brasil`); `author` é o responsável pela alteração. Changeset já aplicado **nunca** é
  editado — corrige-se com um changeset novo. Editar um changeset aplicado muda o checksum e quebra
  o boot de todo ambiente que já o rodou.
- Dados de referência (países, unidades federativas) entram por changeset de **seed idempotente**,
  não por script manual nem por código de aplicação.
- Toda entidade JPA MUST ter changeset correspondente; toda coluna mapeada MUST existir no
  changeset. A divergência é detectada no boot pelo `ddl-auto: validate` e conta como build
  quebrado.

### IV. Validação Acumulada e Ordem Canônica de Serviço

- **Ordem fixa dos métodos no Service:** `findById > search > delete > insert > save > validate`.
  `validate(dto, isInsert)` é privado e sempre o último método da classe.
- Validação **NUNCA** é fail-fast: acumule as mensagens em `ArrayList<String> errors` e lance
  `BusinessException(errors)` uma única vez ao final. O usuário corrige tudo de uma vez, em vez de
  descobrir um erro por submit.
- Proibido `!= null` e `.isEmpty()`: use `ObjectUtil.isNullOrEmpty` e
  `ObjectUtil.isNotNullAndNotEmpty`.
- `@Transactional(rollbackOn = Exception.class)` em toda mutação. Cadastros compostos (entidade +
  agregados) gravam em **uma única transação**, a partir de um único payload.
- Sobrecargas que recebem id `String` delegam para a sobrecarga `UUID`.
- Repository estende `JpaRepository<E, UUID>` **e** `BaseCustomJpaRepository<E>`. A busca vive como
  métodos `default`/`private` na própria interface, na ordem
  `search > countQuery > searchQuery > appendFilters`: HQL montado em `StringBuilder`, parâmetros
  nomeados em `HashMap`, `appendFilters` abrindo sempre com `WHERE 1=1`, execução por
  `executeHql`/`executeCountHql`, retorno em `new PageImpl<>(...)` e projeção por construtor em
  `XxxResponseDTO`. Os aliases do `FROM`/`JOIN` MUST ser os mesmos usados no `SELECT` e no
  `appendFilters` — divergência de alias só falha em runtime.

### V. Contrato REST e Autorização Espelhada na UI

- Controller estende `BaseController`, chama `super.setBodyToExceptionLog(dto, request)` em **todo**
  POST e PUT, documenta com `@Tag` na classe e `@Operation`/`@ApiResponse` nos métodos, e retorna
  **201** em insert, **204** em delete e **200** no restante.
- Busca paginada é **`POST /search`** com `@RequestBody FilterParams` e `@PageableDefault` — nunca
  GET com query params.
- **Autorização é por ação, não por nível.** Cada endpoint declara
  `@PreAuthorize("hasRole('<AÇÃO>')")` com **uma** ação de `AcaoSistema` (`br.com.gems.app.security`)
  — `INSERIR_PARCEIRO`, `EXCLUIR_SOLICITACAO`, `CONCLUIR_SOLICITACAO`. Os perfis de atuação são
  **grupos** do provedor de identidade, e cada grupo carrega uma role **composta** (`ADMIN` para o
  perfil pleno, `PERFIL_<NOME>` para os demais) que o servidor expande em ações no token. O sistema
  nunca atribui role a usuário: apenas o coloca no grupo.
- É **proibido** escrever `hasAnyRole(..., 'ADMIN')`. `ADMIN` é composta de todas as ações e já
  expande; incluí-la à mão é redundante e sugere uma exceção que o modelo não tem. Quem se pegar
  escrevendo isso deve corrigir a **composta do perfil**, não a anotação.
- Dado referencial sem regra de negócio (países, estados, CEP) usa `isAuthenticated()`.
- **Autorização de campo** é o segundo mecanismo, e vive no **service**, não na anotação: quando a
  regra é "este perfil vê o registro mas não vê *este campo*", o service consulta
  `UsuarioAutenticadoService.possuiAcao()` e omite o valor na projeção — em `toDTO`, na busca **e**
  na gravação, para que um payload malicioso não sobrescreva o que o chamador não podia ler. O
  sigilo da comissão do parceiro é a implementação de referência. Aplicá-lo só na UI é defeito, não
  estilo: a API continua devolvendo o valor a quem chamar direto.
- **Estado de fluxo nunca entra pelo corpo da requisição.** Situação, etapa e marco avançam por
  endpoints de ação dedicados (`POST /api/solicitacoes/{id}/concluir`), cada um com a sua própria
  ação de `AcaoSistema`. Aceitar a situação num `PUT` genérico transforma a máquina de estados em
  sugestão e a autorização por transição em ficção.
- A UI **espelha a ação concreta**: `hasRole(Acoes.EXCLUIR_PARCEIRO)` no sinal que controla o botão,
  `data: { roles: [Acoes.INSERIR_PARCEIRO] }` na rota. `core/auth/acoes.ts` é o espelho exato de
  `AcaoSistema.java`, verificado por diff dos dois conjuntos.
- **As duas varreduras que fecham a falha silenciosa são obrigatórias**, e toda rodada nova MUST
  entrar nelas: `CoberturaPreAuthorizeTest` falha se um método mapeado de `br.com.gems.app` não
  tiver `@PreAuthorize`; `routes-coverage.spec.ts` percorre `app.routes.ts`, resolve cada
  `loadChildren` e falha se uma rota guardada não declarar `data.roles`. Sem elas, o endpoint
  esquecido não gera erro — gera acesso.
- O `roleGuard` do frontend falha **fechado**: rota guardada sem `data.roles` é negada, nunca
  liberada.
- Segurança stateless, CSRF desabilitado, `@EnableMethodSecurity`. Público apenas: `OPTIONS`,
  `/actuator/health`, `/api/auth/**` e endpoints anotados com `@PublicEndpoint`. **`/actuator/**`
  como um todo NÃO é público** — `/actuator/metrics/http.server.requests` enumera anonimamente todos
  os URIs da API, com contagem e latência. A documentação OpenAPI (`/v3/api-docs/**`,
  `/swagger-ui/**`) é pública apenas quando `SPRINGDOC_ENABLED` permite, e MUST estar desligada em
  ambiente exposto à internet.
- Rota de terceiro que o projeto publica sem controlar (o `S3Controller` do `gems-aws-web`, cuja
  classe está num jar e não pode receber `@PreAuthorize`) MUST ser negada por regra de caminho no
  `SecurityConfig`, antes de qualquer registro de endpoint público.

### VI. Frontend Modular e Padronizado pela GEMS SDK

- Toda funcionalidade nasce como **módulo de feature próprio** em `src/app/features/<dominio>/`,
  contendo seu arquivo de rotas (`<dominio>.routes.ts`), `models/`, `services/` (store), `list/`,
  `form/` e `view/`. O `app.routes.ts` raiz apenas registra o módulo por `loadChildren`, e o item de
  menu correspondente é adicionado em `core/menu/menu.builder.ts` **com teste**. Nada de componente
  de feature declarado direto na raiz. *(Angular standalone não tem `NgModule`: "módulo" aqui é o
  arquivo de rotas do feature registrado por `loadChildren`.)*
- Componentes standalone; arquivos `.ts`, `.html` e `.css` **sempre separados** (template e style
  inline são proibidos); `inject()` em vez de parâmetros de construtor; `input()`/`output()` em vez
  de `@Input`/`@Output`; `signal()`/`computed()` para estado; `@if`/`@for`/`@switch` em vez de
  `*ngIf`/`*ngFor`/`*ngSwitch`.
- Componentes **nunca** usam `HttpClient` direto: a store estende `GemsBaseStore` com
  `super('api/{recurso}')` (sem barra inicial). `alertService.errorFromApi(err)` em **todo** bloco
  `error:` de chamada HTTP. `alertService.confirm(...)` devolve `SweetAlertResult` — cheque
  `.isConfirmed`.
- Tema definido **só** em `provideGemsTheme()` no `app.config.ts`. Proibido hex hardcoded em CSS de
  componente e proibido sobrescrever variáveis `--gems-*` fora do provider.
- Ícones exclusivamente **Font Awesome 6 Solid**. Botões pelas classes globais definidas em
  `styles.css` (`btn-save`, `btn-cancel`, `btn-novo`, `btn-back`, `btn-secondary`, `btn-danger`).
  Classe de botão que não exista ali renderiza **sem estilo nenhum**, em silêncio.
- Listagem segue o padrão **cabeçalho global + card de filtros + tabela solta**: a `gems-table` é
  elemento próprio da tela, sem card em volta, apenas espaçada do card de filtros. Coluna final de
  ações Visualizar, Editar e Excluir, cada uma visível conforme a ação concreta do usuário.
- Cadastros compostos usam o **wizard** da SDK (`GemsWizardComponent` + `GemsBaseStepComponent`) com
  StateService baseado em signals fornecido no `providers` do orquestrador, steps isolados que só
  conversam pelo StateService, um passo final de Resumo e submit único ao backend. O `gems-wizard` é
  só a barra de progresso: quem renderiza o passo ativo é o orquestrador, com `@switch`.
- **Mensagem de erro de campo usa `app-field-error`** (`shared/forms/field-error/`), não o
  `gems-field-error` da SDK npm 1.1.0 — o `errorMessage` dela é um `computed()` sobre propriedades
  que não são sinais, e por isso **nunca exibe mensagem nenhuma**. Enquanto a SDK não corrigir, usar
  o componente dela é entregar um formulário que recusa salvar sem dizer por quê.
- **Estrutura duplicada em duas features tem um dono só, e ele fica em `shared/` ou `commons/`** —
  nunca dentro de uma delas. `shared/` para o que atravessa features (formulários, componentes,
  modelos compartilhados); `commons/` para constante e função pura. Uma feature importar de outra
  feature é defeito estrutural; `shared/` importar de `features/` é o mesmo defeito, invertido.
- A mesma rota base atende os três modos: `/<recurso>` (lista), `/<recurso>/novo`,
  `/<recurso>/:id/editar` e `/<recurso>/:id` (leitura). A ordem importa: `novo` e `:id/editar` vêm
  **antes** de `:id`.

### VII. Testes como Gate (NÃO NEGOCIÁVEL)

- Regra de negócio nasce **com teste antes** da implementação. Teste de service com JUnit 5 +
  Mockito (o agente do Mockito é anexado pelo Surefire no POM pai).
- Frontend testa com **Vitest** (builder `@angular/build:unit-test`). Funções puras de
  infraestrutura de UI — o construtor de menu é o caso canônico — MUST ter teste.
- **Rede de segurança nova prova-se por mutação**: quebre deliberadamente o alvo, confirme que o
  teste falha, restaure. Teste que passa sem nunca ter falhado não é prova de nada.
- Nenhum teste pode referenciar classe ou componente inexistente. Teste órfão de código removido é
  removido junto, no mesmo commit.
- **Gate de merge:** `.\mvnw.cmd clean install` (reator inteiro, com testes) verde, `npm test`
  verde, `npm run build` verde e aplicação subindo com `ddl-auto: validate`.

### VIII. Especificação Antes de Código, Sem Sobras de Exemplo

- Toda funcionalidade nasce de um ciclo spec-kit:
  `specify > clarify > plan > tasks > analyze > roteiro > implement`. Marcadores
  `[NEEDS CLARIFICATION]` MUST ser resolvidos antes do `plan`.
- A documentação é **proporcional ao risco**. Nova feature, mudança de comportamento, arquitetura ou
  ambiente usa o ciclo completo. Bug que restaura contrato usa teste de regressão e gate. Refatoração
  ou ajuste técnico sem mudança de comportamento usa registro curto (`change-template.md`), tarefas e
  gates; artefato vazio não é criado por cerimônia.
- O `roteiro.md` é o **portão de aprovação humana antes do código**, escrito depois do `analyze` e
  nunca antes. Ele consolida numa peça única o que será desenvolvido em banco, backend e frontend,
  os testes esperados de cada bloco, o **raio de impacto sobre o que já foi entregue**, a divisão de
  tarefas com a delegação por modelo e os critérios de aprovação. Nenhuma linha de código de uma
  feature MUST ser escrita antes de o roteiro dela existir e ter sido aprovado. O template está em
  `.specify/templates/roteiro-template.md`.
- O roteiro **consolida e aponta**, nunca duplica: todo número citado nele MUST bater com a sua fonte
  nos demais artefatos. Documento compilado que diverge da fonte é pior que a ausência dele, porque
  passa a ser lido como verdade sem sê-lo.
- A decisão de delegação MUST ser explícita — coluna `Modelo` na tabela de fases do `plan.md` e nota
  ao fim do `tasks.md`. **Sem delegação é uma decisão válida e preferencial** quando uma sessão nova
  não se amortiza. O critério é custo mínimo sujeito aos gates do Princípio VII: modelo mais capaz
  somente onde erro silencioso custa caro; modelos menores onde há espelho e teste forte. Toda
  escolha do modelo mais caro MUST ser justificada por risco material.
- Executor delegado recebe listas fechadas `READ`, `EDIT`, `CREATE` e `TEST`. Se o pacote for
  insuficiente, MUST parar e reportar; explorar o repositório para completar contexto é violação de
  escopo. Sessão existente SHOULD ser retomada em vez de recriada.
- Durante a iteração, execute o gate focalizado da área. O gate completo continua obrigatório no
  fechamento, mas SHOULD rodar uma vez pelo orquestrador para evitar repetição de contexto e logs.
- As especificações de requisito em `utils/documentação do projeto/` são a fonte de entrada. Quando
  divergirem do código já consolidado, **o código vence** e a divergência é registrada no spec da
  feature.
- Código de exemplo, scaffold ou domínio de template MUST ser removido **por inteiro** — backend,
  frontend, testes e changeset — assim que deixar de ser referência ativa. O projeto não carrega
  domínio fictício em nenhuma branch de longa duração.

### IX. Configuração de Ambiente é Código

O schema do banco já obedecia a isso desde o Princípio III. Este princípio estende a mesma exigência
ao resto do ambiente.

- **Todo ambiente nasce de arquivo versionado.** Realm do provedor de identidade, criação dos bancos,
  imagens, topologia de serviços e proxy reverso vivem em `environments/`, sob controle de versão.
  Subir um ambiente novo MUST ser `docker compose up`, sem passo manual intermediário.
- **Nenhuma alteração de ambiente é feita por console ou script avulso.** Mudança de configuração do
  provedor de identidade MUST ser feita editando o arquivo de realm e reaplicando. Alteração feita
  direto no console administrativo é drift, não configuração — e MUST ser desfeita pela reaplicação.
- **A reaplicação MUST reconciliar, não apenas aplicar diferenças de arquivo.** A ferramenta de
  configuração roda com o cache de checksum desligado, para que o estado do ambiente convirja para o
  arquivo mesmo quando o arquivo não mudou. Com o cache ligado, uma alteração manual sobrevive
  indefinidamente e em silêncio, porque a ferramenta pula o reimport quando o arquivo não mudou.
- **Segredo nunca é versionado.** Senha, client secret e credencial de nuvem chegam por variável de
  ambiente, com um `.env.example` versionado servindo de contrato e o `.env` real fora do Git.
  Nenhum valor real de produção pode existir no repositório, em nenhum arquivo.
- **Dado de produção MUST ter backup verificado.** Backup nunca restaurado é hipótese, não backup: o
  ensaio de restore é parte do gate de entrega, não da documentação. Onde a integridade depende de
  dois armazenamentos distintos, o backup MUST ser tratado como unidade e restaurado em conjunto.
- **Exposição de rede é mínima e explícita.** Em ambiente publicado, apenas o proxy reverso expõe
  porta ao host; banco, provedor de identidade e aplicação conversam pela rede interna.
- **Superfície administrativa não é publicada.** O console do provedor de identidade e qualquer
  endpoint de diagnóstico que enumere a aplicação MUST ser fechados no proxy e alcançados por túnel,
  não por allowlist de IP — endereço residencial muda, e a regra que depende dele apodrece em
  silêncio. O ambiente publicado MUST responder com os cabeçalhos de segurança (HSTS, `nosniff`,
  `X-Frame-Options`, `Referrer-Policy`, CSP) e MUST ser verificado **de fora da máquina** antes de
  ser considerado entregue.

## Stack e Restrições Técnicas

| Camada | Tecnologia |
| :--- | :--- |
| Backend | Java 21, Spring Boot 4.1, Maven multi-módulo, GEMS SDK, Liquibase |
| Banco | PostgreSQL 16 |
| Frontend | Angular 22 standalone, TypeScript 6, `angular-gems-sdk` 1.1.0, Vitest, Prettier (**não há ESLint**) |
| Segurança | Keycloak 26 como resource server JWT |
| Arquivos | AWS S3 (LocalStack no ambiente local) |
| DevOps | Docker Compose, GitHub Actions |

Restrições operacionais que MUST ser respeitadas:

- No Windows, o build usa `.\mvnw.cmd`; use `-pl <modulo>` para mirar um módulo e `-am` para
  recompilar suas dependências no próprio reator.
- `npm install` exige `--legacy-peer-deps`: o `angular-gems-sdk@1.1.0` declara peer
  `@angular/common@^20` e o projeto está no Angular 22. Sem a flag, `ERESOLVE` aborta o install.
- Infra local em `environments/local`: **`docker compose up -d` e nada mais**. O serviço one-shot
  `keycloak-config` aplica o realm declarado em `environments/shared/keycloak/` assim que o Keycloak
  fica saudável — não há script manual a rodar depois. O Keycloak leva cerca de 80 segundos no
  primeiro boot, e é por isso que a dependência é por `service_healthy`, nunca só por `depends_on`.
- O serviço `keycloak-config` MUST rodar com `IMPORT_CACHE_ENABLED=false` em todos os ambientes
  (Princípio IX).
- `docker compose down -v` é **proibido**: `init-postgres.sh` cria `app-db` e `keycloak-db` no mesmo
  volume, então limpá-lo destrói o banco de aplicação junto com o do Keycloak — e as senhas dos
  usuários existem **apenas** no `keycloak-db`.

## Fluxo de Trabalho e Portões de Qualidade

- Branch a partir de `main` no padrão `feature/<descricao>` ou `bugfix/<descricao>`.
- PR para `main` com título curto e corpo contendo: problema resolvido, recursos criados, recursos
  removidos ou depreciados, e pendências.
- Antes do PR: `.\mvnw.cmd clean install` na raiz do backend, `npm test` e `npx prettier --write .`
  no frontend.
- Nenhum PR é aprovado com o gate do Princípio VII vermelho.
- Alteração de infraestrutura ou de configuração de ambiente segue o **mesmo** ciclo spec-kit do
  Princípio VIII. Ela muda como o sistema é publicado e operado, e merece a mesma rastreabilidade de
  uma mudança de comportamento.

## Governance

Esta constituição **prevalece sobre qualquer outra prática documentada**, inclusive `CLAUDE.md`,
`AGENTS.md`, `CODEX.md`, `GEMINI.md`, `.cursor/rules/`, `.github/copilot-instructions.md` e os
arquivos em `.gems-ai/`. Quando um desses arquivos divergir da constituição, **ele** é corrigido —
não a constituição.

Emendas exigem:

1. Registro no **Sync Impact Report** no topo deste arquivo.
2. Incremento **semântico** de versão: MAJOR para remoção ou redefinição incompatível de princípio,
   MINOR para novo princípio ou expansão material de orientação, PATCH para clarificação, correção
   de redação ou refinamento não semântico.
3. Atualização dos arquivos de agente afetados **na mesma alteração**.

Toda revisão de PR MUST verificar conformidade com estes princípios. Complexidade adicional MUST ser
justificada por escrito no `plan.md` da feature — ausência de justificativa é motivo suficiente para
recusar o PR.

**Version**: 1.0.0 | **Ratified**: 2026-09-05 | **Last Amended**: 2026-09-05
