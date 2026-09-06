# Frontend

- Angular 22 standalone; arquivos `.ts`, `.html` e `.css` separados.
- Feature autocontida em `features/<dominio>` e registrada na raiz somente por `loadChildren`.
- `shared/` não importa `features/`; uma feature não importa outra feature.
- Estado usa signals e stores `GemsBaseStore`; componente não usa `HttpClient` diretamente.
- Autorização espelha a ação concreta do backend em rota, menu e botões.
- Testes usam Vitest; não Jasmine, Karma ou Jest.

Features em `src/app/features/`: `parceiro`, `solicitacao`, `home`, `acesso-negado`.

Armadilhas obrigatórias — cada uma já produziu defeito silencioso na `angular-gems-sdk` 1.1.0:

- `gems-select`: `placeholder` e valor inicial `null`.
- `gems-select`/`gems-input-document` não têm `hint`.
- RG usa `gems-input-text` + `appRgMask`; `maskType="rg"` é proibido.
- Dentro de `gems-form-card`, popover usa `position: fixed`.
- Erro de campo usa `app-field-error`, nunca `gems-field-error`.
- Fora da tabela use `btn-secondary`, não `btn-default`.

Leia apenas as regras GEMS da tela afetada e use `features/parceiro` como referência. Gates:

```powershell
npm test
npm run build
```
