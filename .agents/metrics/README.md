# MÃ©trica mÃ­nima de custo de agentes

O repositÃ³rio nÃ£o versiona telemetria bruta. Registre cada lote com:

```powershell
.\scripts\registrar-custo-agente.ps1 `
  -Agent codex -Model sol -Lote 001-A -Session resumed `
  -InputTokens 100000 -CachedTokens 85000 -OutputTokens 5000 `
  -BilledCost 0 -Outcome accepted -Gate "mvn test" -ReviewMinutes 10
```

A mÃ©trica de decisÃ£o Ã© `BilledCost / accepted`, complementada por retrabalho e minutos de revisÃ£o.
O CSV padrÃ£o fica ignorado em `.agents/metrics/custos.csv`.

## PrÃ©-voo obrigatÃ³rio do Kilo

O `kilo.json` limita o projeto ao DeepSeek V4 Flash, mas nÃ£o substitui controles mantidos pelo
cliente ou pelo provedor. Antes de cada sessÃ£o:

1. abra um worktree novo e atualizado; nunca retome worktree criado antes da configuraÃ§Ã£o econÃ´mica;
2. use **Reset model** no seletor do agente e confirme `deepseek-v4-flash-0731`;
3. confirme no painel do Kilo que Memory, Codebase Indexing, Autocomplete e todos os MCPs estÃ£o
   desligados;
4. use uma chave OpenRouter exclusiva, sem segredo versionado, com allowlist somente do modelo acima
   e limite rÃ­gido de **US$ 2 por dia**;
5. inicie em `ask`; aprove explicitamente cada ediÃ§Ã£o e comando de shell;
6. no primeiro uso apÃ³s mudanÃ§a de configuraÃ§Ã£o, execute um smoke test somente leitura de no
   mÃ¡ximo cinco passos e aceite apenas custo inferior a **US$ 0,10**.

O seletor de modelo persistido, o estado da Memory e o teto da chave sÃ£o externos ao repositÃ³rio.
Uma sessÃ£o nÃ£o comeÃ§a enquanto esses trÃªs controles nÃ£o forem conferidos. Registre o smoke test
com `registrar-custo-agente.ps1`; nunca registre chave, prompt integral ou identificador sensÃ­vel.
