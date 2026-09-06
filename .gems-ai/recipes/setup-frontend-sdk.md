# Recipe: Setup Frontend — Angular GEMS SDK

**Objetivo:** Configurar um novo projeto Angular com a GEMS SDK via GitHub Packages npm, design tokens e interceptors obrigatórios.

**Rules relacionadas:**
- [signals-standards.md](../rules/frontend-angular/signals-standards.md)
- [visual-standards.md](../rules/frontend-angular/visual-standards.md)
- [http-interceptors.md](../rules/frontend-angular/http-interceptors.md)
- [auth.md](../rules/frontend-angular/auth.md)
- [organization.md](../rules/frontend-angular/organization.md)

---

## Passo 1: Configurar GitHub Packages npm

```bash
# .npmrc na raiz do projeto
@gabriel-mdias:registry=https://npm.pkg.github.com
//npm.pkg.github.com/:_authToken=${GITHUB_TOKEN}
```

`GITHUB_TOKEN` = Personal Access Token GitHub com permissão `read:packages`.

## Passo 2: Instalar a SDK

```bash
npm install gems-sdk --legacy-peer-deps
npm install angular-oauth2-oidc --legacy-peer-deps
npm install animate.css
```

`--legacy-peer-deps` pode ser necessário por peer-deps do Angular.

## Passo 3: app.config.ts Completo

```typescript
// src/app/app.config.ts
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideOAuthClient } from 'angular-oauth2-oidc';
import { provideGemsTheme, loadingInterceptor } from 'gems-sdk';
import { routes } from './app.routes';
import { correlationIdInterceptor } from './core/interceptors/correlation-id.interceptor';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withComponentInputBinding()),
    provideAnimations(),
    provideHttpClient(
      withFetch(),
      withInterceptors([loadingInterceptor, correlationIdInterceptor])
    ),
    provideOAuthClient({
      resourceServer: {
        allowedUrls: [environment.apiBaseUrl],
        sendAccessToken: true
      }
    }),
    provideGemsTheme({
      primary:    '#1e3a5f',
      secondary:  '#3b82f6',
      tertiary:   '#6366f1',
      background: '#f8fafc'
    })
  ]
};
```

## Passo 4: app.component.html

```html
<!-- src/app/app.component.html -->
<gems-loading></gems-loading>
<router-outlet></router-outlet>
```

`gems-loading` exibe o loading global gerenciado pelo `loadingInterceptor`.

## Passo 5: Estrutura de Pastas

```
src/app/
├── core/
│   ├── auth/
│   │   ├── auth.config.ts
│   │   ├── auth.guard.ts
│   │   └── session.service.ts
│   └── interceptors/
│       └── correlation-id.interceptor.ts
├── features/
│   └── {modulo}/
│       ├── {modulo}.routes.ts
│       ├── models/
│       │   ├── {entidade}.model.ts
│       │   └── {entidade}-filter.model.ts
│       ├── services/
│       │   └── {entidade}.store.ts
│       ├── list/
│       │   ├── {entidade}-list.component.ts
│       │   └── {entidade}-list.component.html
│       └── form/
│           ├── {entidade}-form.component.ts
│           └── {entidade}-form.component.html
├── app.component.ts
├── app.component.html
├── app.config.ts
└── app.routes.ts
```

## Passo 6: Configurar angular.json (styles)

```json
// angular.json → projects.{app}.architect.build.options
{
  "styles": [
    "node_modules/animate.css/animate.min.css",
    "node_modules/gems-sdk/styles/gems-theme.css",
    "src/styles.css"
  ]
}
```

## Passo 7: environments/

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  oidc: {
    issuer:      'https://your-idp.example.com/realms/{realm}',
    clientId:    '{client-id}',
    redirectUri: window.location.origin + '/callback',
    scope:       'openid profile email'
  }
};
```

```typescript
// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiBaseUrl: 'https://api.{app}.com.br',
  oidc: {
    issuer:      'https://your-idp.example.com/realms/{realm}',
    clientId:    '{client-id}',
    redirectUri: 'https://{app}.com.br/callback',
    scope:       'openid profile email'
  }
};
```

---

## Checklist de Conformidade

- [ ] `.npmrc` com registry e token do GitHub Packages.
- [ ] `provideGemsTheme()` configurado com as cores do projeto.
- [ ] `loadingInterceptor` + `correlationIdInterceptor` em `withInterceptors([])`.
- [ ] `withFetch()` presente no `provideHttpClient`.
- [ ] `<gems-loading>` no `app.component.html`.
- [ ] `animate.css` nos estilos do `angular.json`.
- [ ] `gems-sdk/styles/gems-theme.css` nos estilos do `angular.json`.
- [ ] Estrutura de pastas: `core/` (auth + interceptors) + `features/{modulo}/`.
- [ ] `environment.ts` com `apiBaseUrl` e configuração OIDC.
- [ ] Nenhum componente usa `HttpClient` diretamente — sempre via `GemsBaseStore`.
