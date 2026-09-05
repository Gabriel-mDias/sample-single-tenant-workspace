---
stack: frontend-angular
module: gems-sdk, @angular/common/http
severity: mandatory
see-also: [stores.md, auth.md]
---

# HTTP Interceptors — Angular / gems-sdk

> **TL;DR:**
> - Dois interceptors obrigatórios: `loadingInterceptor` (da gems-sdk) e `correlationIdInterceptor` (local).
> - `loadingInterceptor`: loading global automático — **nunca** criar um interceptor de loading próprio.
> - `correlationIdInterceptor`: gera UUID v4 por requisição, define header `X-Correlation-Id`.
> - Registrar via `withInterceptors([loadingInterceptor, correlationIdInterceptor])` em `app.config.ts`.
> - `withFetch()` obrigatório em Angular 19+.
> - **NUNCA** usar `HttpClient` diretamente em componentes — sempre via `GemsBaseStore`.

## 1. Registro em app.config.ts

```typescript
// app.config.ts
import {
  provideHttpClient,
  withInterceptors,
  withFetch
} from '@angular/common/http';
import { loadingInterceptor } from 'gems-sdk';
import { correlationIdInterceptor } from './core/interceptors/correlation-id.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withFetch(),
      withInterceptors([
        loadingInterceptor,    // Interceptor da SDK — loading global automático
        correlationIdInterceptor
      ])
    ),
    // ...
  ]
};
```

Ordem: `loadingInterceptor` **antes** de `correlationIdInterceptor`.

---

## 2. loadingInterceptor (gems-sdk)

Fornecido pela SDK. **Não crie um interceptor de loading próprio.**

- Exibe o estado de carregamento global enquanto há requisições HTTP em voo.
- Integra com o componente de loading global da SDK (quando registrado no `app.component.html`).
- Contabiliza requisições com contador interno — fecha o loading somente quando todas completam.

```html
<!-- app.component.html — habilitar o loading global da SDK -->
<gems-loading></gems-loading>
<router-outlet></router-outlet>
```

---

## 3. correlationIdInterceptor

Gera e propaga o `X-Correlation-Id` por requisição.

```typescript
// src/app/core/interceptors/correlation-id.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';

export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  const correlationId = generateUuidV4();
  const cloned = req.clone({
    headers: req.headers.set('X-Correlation-Id', correlationId)
  });
  return next(cloned);
};

function generateUuidV4(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
```

- Geração client-side (sem dependência de `crypto.randomUUID()` para maior compatibilidade).
- O backend (`gems-rest-common`) lê este header, grava no MDC e o inclui na resposta.
- Ver `rules/backend-java/rest-common.md` para o lado do servidor.

---

## 4. Tratamento de Erros HTTP Global (Opcional)

Quando houver necessidade de interceptar erros 401/403 globalmente (redirecionar para login):

```typescript
// src/app/core/interceptors/auth-error.interceptor.ts
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';

export const authErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
```

Registrar após `correlationIdInterceptor` no array de `withInterceptors([])`.

---

## 5. Regras

| Regra | Detalhe |
| :--- | :--- |
| `loadingInterceptor` da SDK obrigatório | Não criar interceptor de loading customizado |
| `X-Correlation-Id` gerado no cliente | UUID v4 por requisição; backend persiste no MDC |
| Interceptors funcionais (`HttpInterceptorFn`) | Nunca classe com `@Injectable` + `intercept()` (padrão Angular <15) |
| `withFetch()` obrigatório | Angular 19+ usa Fetch API por padrão; precisa estar explícito |
| HttpClient nunca em componentes | Sempre via `GemsBaseStore.get/post/put/delete` |

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Criar interceptor de loading customizado | Usar `loadingInterceptor` já fornecido pela gems-sdk |
| `class LoadingInterceptor implements HttpInterceptor` | `export const loadingInterceptor: HttpInterceptorFn = ...` (funcional) |
| Omitir `withFetch()` no `provideHttpClient()` | `provideHttpClient(withFetch(), withInterceptors([...]))` obrigatório |
| `sessionStorage.setItem('correlationId', uuid)` | UUID gerado por requisição no interceptor, sem persistência |
| `HttpClient` injetado diretamente no componente | `GemsBaseStore.get/post/put/delete` — centraliza HTTP |
