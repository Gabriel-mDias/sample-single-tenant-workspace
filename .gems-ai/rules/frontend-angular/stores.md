---
stack: frontend-angular
module: gems-sdk (GemsBaseStore)
severity: mandatory
see-also: [signals-standards.md, lists.md, forms.md]
---

# Stores (HTTP Client Layer) — Angular / gems-sdk

> **TL;DR:**
> - Toda Store estende `GemsBaseStore` (gems-sdk) — **NUNCA** `HttpClient` diretamente nos componentes.
> - `@Injectable({ providedIn: 'root' })` obrigatório. Arquivo: `features/{modulo}/services/{modulo}.store.ts`.
> - `super('api/{recurso-kebab-case}')` no construtor — **sem** barra inicial.
> - Ordem: `getAll()` > `getById()` > `search()` > `create()` > `update()` > `delete{Entidade}()`.
> - `search()` retorna `Observable<{ content: T[]; totalElements: number }>`.
> - IDs sempre `string` — nunca `UUID` ou `number` no frontend.
> - Tipagem explícita com generics em todos os métodos.

## 1. Declaração da Store

```typescript
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { GemsBaseStore, Pageable } from 'gems-sdk';
import { {Entidade}Model } from '../../../models/{entidade}.model';
import { {Entidade}FilterModel } from '../../../models/{entidade}-filter.model';
import { {Entidade}ResponseModel } from '../../../models/{entidade}-response.model';

@Injectable({
  providedIn: 'root'
})
export class {Entidade}Store extends GemsBaseStore {

  constructor() {
    super('api/{recurso-kebab-case}');   // Path base — sem barra inicial
  }
```

### Regras de declaração:
- **Estende** `GemsBaseStore` (não `BaseStore` — nome corrigido no SDK).
- `@Injectable({ providedIn: 'root' })` sempre.
- `constructor()` chama `super('api/{recurso}')` sem barra inicial.
- Arquivo em `features/{modulo}/services/{modulo}.store.ts`.

---

## 2. Ordem Fixa dos Métodos

1. `getAll()` — quando necessário
2. `getById(id)` / `findById(id)`
3. `search(filter, pageable)`
4. `create(dto)`
5. `update(id, dto)`
6. `delete{Entidade}(id)`
7. Métodos de ação específica de negócio (quando aplicável)

---

## 3. Padrão de Cada Método

### getAll
```typescript
getAll(): Observable<{Entidade}Model[]> {
  return this.get<{Entidade}Model[]>('');
}
```

### getById
```typescript
getById(id: string): Observable<{Entidade}Model> {
  return this.get<{Entidade}Model>(`/${id}`);
}
```

### search — retorna sempre `{ content, totalElements }`
```typescript
search(filter: {Entidade}FilterModel, pageable: Pageable): Observable<{ content: {Entidade}ResponseModel[]; totalElements: number }> {
  return this.post<{ content: {Entidade}ResponseModel[]; totalElements: number }, {Entidade}FilterModel>(
    '/search',
    filter,
    { pageable }
  );
}
```

### create
```typescript
create(dto: {Entidade}Model): Observable<{Entidade}Model> {
  return this.post<{Entidade}Model, {Entidade}Model>('', dto);
}
```

### update
```typescript
update(id: string, dto: {Entidade}Model): Observable<{Entidade}Model> {
  return this.put<{Entidade}Model, {Entidade}Model>(`/${id}`, dto);
}
```

### delete
```typescript
delete{Entidade}(id: string): Observable<void> {
  return this.delete<void>(`/${id}`);
}
```

---

## 4. Métodos de Ação de Negócio

Para operações além do CRUD:

```typescript
suspenderAcesso(id: string): Observable<void> {
  return this.put<void, void>(`/suspensao/${id}`, undefined as any);
}

ativarAcesso(id: string): Observable<void> {
  return this.put<void, void>(`/ativacao/${id}`, undefined as any);
}

importarLote(arquivo: FormData): Observable<{ importados: number }> {
  return this.post<{ importados: number }, FormData>('/importar', arquivo);
}
```

- PUT sem body: `undefined as any` como segundo argumento.
- Nomes em camelCase refletindo a ação de negócio.
- Path em kebab-case: `/suspensao`, `/ativacao`.

---

## 5. Tipagem

- **Sempre** tipar com generics: `this.get<TipoRetorno>()`, `this.post<TipoRetorno, TipoBody>()`.
- IDs são sempre `string` no frontend.
- Evite `any` — use interfaces tipadas ou `unknown`.
- `search()` retorna **sempre** `Observable<{ content: T[]; totalElements: number }>`.

---

## 6. Template Completo

```typescript
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { GemsBaseStore, Pageable } from 'gems-sdk';
import { {Entidade}Model } from '../../../models/{entidade}.model';
import { {Entidade}FilterModel } from '../../../models/{entidade}-filter.model';

@Injectable({
  providedIn: 'root'
})
export class {Entidade}Store extends GemsBaseStore {

  constructor() {
    super('api/{recurso}');
  }

  getAll(): Observable<{Entidade}Model[]> {
    return this.get<{Entidade}Model[]>('');
  }

  getById(id: string): Observable<{Entidade}Model> {
    return this.get<{Entidade}Model>(`/${id}`);
  }

  search(filter: {Entidade}FilterModel, pageable: Pageable): Observable<{ content: {Entidade}Model[]; totalElements: number }> {
    return this.post<{ content: {Entidade}Model[]; totalElements: number }, {Entidade}FilterModel>('/search', filter, { pageable });
  }

  create(dto: {Entidade}Model): Observable<{Entidade}Model> {
    return this.post<{Entidade}Model, {Entidade}Model>('', dto);
  }

  update(id: string, dto: {Entidade}Model): Observable<{Entidade}Model> {
    return this.put<{Entidade}Model, {Entidade}Model>(`/${id}`, dto);
  }

  delete{Entidade}(id: string): Observable<void> {
    return this.delete<void>(`/${id}`);
  }
}
```

---

## 7. Formatação

- Aspas simples `'` para strings.
- Template literals `` ` `` para paths com variáveis.
- `;` em toda declaração.
- Uma linha em branco entre cada método.
- Imports: Angular → gems-sdk → rxjs → models locais.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `HttpClient` injetado diretamente no componente ou Store | `GemsBaseStore.get/post/put/delete` — nunca `HttpClient` diretamente |
| `super('/api/recurso')` (com barra inicial) | `super('api/recurso')` — sem barra inicial |
| `extends BaseStore` (nome antigo) | `extends GemsBaseStore` |
| `id: number` no tipo de retorno | `id: string` — IDs sempre `string` no frontend |
| Método sem tipagem de generics: `this.post(...)` | `this.post<TReturn, TBody>(path, body)` — sempre tipado |
