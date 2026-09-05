---
stack: frontend-angular
module: angular-oauth2-oidc
severity: mandatory
see-also: [http-interceptors.md, stores.md]
---

# Autenticação e Autorização — Angular / gems-sdk

> **TL;DR:**
> - Fluxo OIDC/PKCE via `angular-oauth2-oidc`. Backend valida JWT como Resource Server.
> - `authGuard`: `CanActivateFn` — verifica `session.isAuthenticated`, chama `session.login()` (PKCE) se não autenticado.
> - `SessionService`: encapsula token, claims, roles e logout. **NUNCA** acessar `localStorage` diretamente.
> - `hasRole(role)`: verificação de autorização em template (`@if (session.hasRole('ROLE'))`) e no `roleGuard`.
> - Guards: funcionais (`CanActivateFn`) — **nunca** classe com `implements CanActivate`.

## 1. Dependência

```bash
npm install angular-oauth2-oidc
```

---

## 2. Configuração OIDC em app.config.ts

```typescript
// app.config.ts
import { provideOAuthClient, OAuthModule } from 'angular-oauth2-oidc';

export const appConfig: ApplicationConfig = {
  providers: [
    provideOAuthClient({
      resourceServer: {
        allowedUrls: [environment.apiBaseUrl],
        sendAccessToken: true    // Injeta Bearer token automaticamente nas chamadas à API
      }
    }),
    // ...
  ]
};
```

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  oidc: {
    issuer:       'https://your-idp.example.com/realms/{realm}',
    clientId:     '{client-id}',
    redirectUri:  window.location.origin + '/callback',
    scope:        'openid profile email offline_access'
  }
};
```

---

## 3. SessionService

Encapsula todo acesso ao token e claims:

```typescript
// src/app/core/auth/session.service.ts
import { Injectable, inject, computed, signal } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { Router } from '@angular/router';
import { authConfig } from './auth.config';

@Injectable({ providedIn: 'root' })
export class SessionService {

  private oauthService = inject(OAuthService);
  private router = inject(Router);

  constructor() {
    this.oauthService.configure(authConfig);
    this.oauthService.loadDiscoveryDocumentAndTryLogin();
  }

  get isAuthenticated(): boolean {
    return this.oauthService.hasValidAccessToken();
  }

  get claims(): Record<string, any> {
    return this.oauthService.getIdentityClaims() ?? {};
  }

  get userName(): string {
    return this.claims['name'] ?? this.claims['preferred_username'] ?? '';
  }

  get email(): string {
    return this.claims['email'] ?? '';
  }

  get roles(): string[] {
    const resource = this.claims['resource_access']?.[authConfig.clientId!];
    return resource?.roles ?? [];
  }

  hasRole(role: string): boolean {
    return this.roles.includes(role);
  }

  login(): void {
    this.oauthService.initCodeFlow();
  }

  logout(): void {
    this.oauthService.logOut();
  }
}
```

```typescript
// src/app/core/auth/auth.config.ts
import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

export const authConfig: AuthConfig = {
  issuer:                         environment.oidc.issuer,
  clientId:                       environment.oidc.clientId,
  redirectUri:                    environment.oidc.redirectUri,
  scope:                          environment.oidc.scope,
  responseType:                   'code',
  useSilentRefresh:               false,
  showDebugInformation:           !environment.production,
  requireHttps:                   environment.production,
  clearHashAfterLogin:            false
};
```

---

## 4. AuthGuard

Guard funcional (Angular 15+):

```typescript
// src/app/core/auth/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from './session.service';

export const authGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.isAuthenticated) return true;

  session.login();    // Dispara PKCE — retorna ao redirectUri após autenticação
  return false;
};
```

Aplicar nas rotas protegidas:

```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '{modulo}', loadChildren: () => import('./features/{modulo}/{modulo}.routes') }
    ]
  },
  { path: 'login', component: LoginComponent },
  { path: 'callback', component: CallbackComponent }
];
```

---

## 5. RoleGuard

Para proteger rotas por perfil:

```typescript
// src/app/core/auth/role.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { SessionService } from './session.service';

export const roleGuard = (requiredRole: string): CanActivateFn => () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.hasRole(requiredRole)) return true;

  router.navigate(['/acesso-negado']);
  return false;
};
```

```typescript
// Uso na rota:
{
  path: 'admin',
  canActivate: [authGuard, roleGuard('ADMIN')],
  loadComponent: () => import('./admin/admin.component')
}
```

---

## 6. Uso no Template

```typescript
// No componente, injete SessionService
sessionService = inject(SessionService);
```

```html
<!-- Exibir nome do usuário -->
<span>{{ sessionService.userName }}</span>

<!-- Condicional por role -->
@if (sessionService.hasRole('GESTOR')) {
  <button class="btn-novo" routerLink="/admin">Área Administrativa</button>
}

<!-- Botão de logout -->
<button (click)="sessionService.logout()">Sair</button>
```

---

## 7. Regras

| Regra | Detalhe |
| :--- | :--- |
| PKCE obrigatório | `responseType: 'code'` — nunca implicit flow |
| `localStorage` só via `OAuthService` | Nunca acessar `localStorage.getItem('access_token')` diretamente |
| Bearer token automático | `sendAccessToken: true` no `provideOAuthClient` + `allowedUrls` configuradas |
| Roles via claims do JWT | `resource_access.{clientId}.roles` — convenção Keycloak (adaptar para outro IdP) |
| Guards funcionais | `CanActivateFn` — nunca classe com `implements CanActivate` |
| Logout via `oauthService.logOut()` | Limpa token e redireciona para `end_session_endpoint` do IdP |

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `responseType: 'token'` (implicit flow) | `responseType: 'code'` — PKCE obrigatório |
| `localStorage.getItem('access_token')` diretamente | `oauthService.getAccessToken()` |
| `class AuthGuard implements CanActivate` | `export const authGuard: CanActivateFn = (route, state) => ...` (funcional) |
| `Bearer ${localStorage.getItem('token')}` no header | `sendAccessToken: true` + `allowedUrls` no `provideOAuthClient` |
| `oauthService.getIdentityClaims()['roles']` | `resource_access.{clientId}.roles` — estrutura Keycloak de roles |
