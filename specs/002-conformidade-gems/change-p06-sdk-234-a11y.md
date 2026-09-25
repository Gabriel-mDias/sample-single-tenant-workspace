# Change: consumir Angular GEMS SDK 2.3.4 e corrigir contraste

**Data**: 2026-09-25 · **Tipo**: acessibilidade

## Intenção

A auditoria autenticada de P06 encontrou um `aria-sort` inválido na tabela da SDK 2.3.2, contraste insuficiente da paginação quando há registros e um acionador de data que permanecia no fluxo de teclado em campos desabilitados. As correções foram publicadas nas versões 2.3.3 e 2.3.4 da Angular GEMS SDK. O Sample fixa a 2.3.4 e usa o token de texto principal na mensagem de acesso negado para atender ao contraste mínimo.

## Fronteira

- **EDIT**: versão exata da Angular SDK nos manifests do frontend e cor do parágrafo em `acesso-negado.component.css`.
- **Fora de escopo**: contratos backend, perfis e regras de autorização.

## Prova e risco

- `npm test -- --watch=false`: 27 arquivos, 126 testes, exit 0.
- `npm run build`: exit 0, com aviso preexistente de orçamento do bundle inicial.
- Navegador autenticado com PostgreSQL/Keycloak locais: axe-core WCAG 2/2.1 A/AA em 12 combinações de seis rotas básicas e dois temas, e em outras 12 combinações de listas com registros, edição e visualização nos dois temas. Nenhuma violação nas 24 combinações estáveis; os dois formulários com erros de validação também passaram em auditoria anterior.
- A prova de acessibilidade é uma amostra dos estados e dados exercitados. Ela não substitui a assinatura integral de P06.

As mudanças da tabela e do seletor de data pertencem à Angular SDK 2.3.4. Reverter o pacote para 2.3.2 reintroduz as violações confirmadas nas telas do Sample.
