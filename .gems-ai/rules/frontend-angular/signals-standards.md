---
stack: frontend-angular
module: @angular/core
severity: mandatory
see-also: [forms.md, lists.md, stores.md, stepper.md]
---

# Signals & Modern Angular — Angular 20+

> **TL;DR:**
> - Angular 20+: `standalone: true` obrigatório. **NUNCA** `NgModules`.
> - Signals: use `input()`, `output()`, `model()`, `signal()`, `computed()` — **NUNCA** `@Input`/`@Output` decorators.
> - Injeção: `inject()` no corpo da classe — **NUNCA** `constructor` com parâmetros.
> - Controle de fluxo: `@if` / `@for` / `@switch` — **NUNCA** `*ngIf` / `*ngFor` / `*ngSwitch`.
> - Two-way binding: `[(ngModel)]` ou `model()` signal.
> - Lazy loading: `loadComponent()` em todas as rotas.

## 1. Standalone Components (Obrigatório)

```typescript
@Component({
  selector: 'app-{entidade}-form',
  standalone: true,     // Sempre
  imports: [            // Importa diretamente o que usa
    ReactiveFormsModule,
    RouterModule,
    FormCardComponent,
    InputMaskComponent
  ],
  templateUrl: './{entidade}-form.component.html'
})
export class {Entidade}FormComponent { }
```

**Nunca** use `NgModule`. Cada componente é auto-suficiente via `imports`.

---

## 2. Signals — Declaração de Propriedades

### Signals de estado interno
```typescript
export class {Entidade}FormComponent {
  // Signals de estado
  isLoading = signal(false);
  isEdit = signal(false);

  // Computed (derivado de outros signals)
  pageTitle = computed(() => this.isEdit() ? 'Editar {Entidade}' : 'Nova {Entidade}');
  pageSubtitle = computed(() => this.isEdit() ? 'Atualize os dados.' : 'Preencha os dados abaixo.');

  // Mutação
  toggleLoading() {
    this.isLoading.set(!this.isLoading());
  }
}
```

### Inputs de componente
```typescript
export class {Entidade}CardComponent {
  // CORRETO — Angular 20+
  title = input.required<string>();
  subtitle = input<string>('');
  isReadOnly = input<boolean>(false);

  // Alias (quando o nome público difere do interno)
  entidadeId = input.required<string>({ alias: 'id' });
}
```

### Outputs de componente
```typescript
export class {Entidade}CardComponent {
  // CORRETO — Angular 20+
  selected = output<{Entidade}Model>();
  deleted = output<string>();   // Emite o ID

  onSelect(item: {Entidade}Model) {
    this.selected.emit(item);
  }
}
```

### Two-way binding (model signal)
```typescript
export class ToggleComponent {
  // Two-way binding — substitui @Input + @Output com EventEmitter
  value = model<boolean>(false);
}

// No template pai:
// <app-toggle [(value)]="meuSignal" />
```

---

## 3. Injeção de Dependências — inject()

```typescript
export class {Entidade}ListComponent {
  // CORRETO — inject() no corpo da classe
  private store = inject({Entidade}Store);
  private alertService = inject(AlertService);
  private navigationService = inject(NavigationService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  // Signals de estado
  entidades = signal<{Entidade}Model[]>([]);
  isLoading = signal(false);
}
```

**Nunca** use `constructor(private store: {Entidade}Store)` — use `inject()`.

---

## 4. Controle de Fluxo no Template

```html
<!-- CORRETO — Angular 17+ / 20+ -->
@if (isLoading()) {
  <div class="loading-state">
    <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando...
  </div>
} @else {
  <gems-table [data]="entidades()" ... />
}

@for (item of entidades(); track item.id) {
  <div class="item">{{ item.nome }}</div>
} @empty {
  <p>Nenhum item encontrado.</p>
}

@switch (situacao()) {
  @case ('ATIVO') { <span class="badge bg-success">Ativo</span> }
  @case ('INATIVO') { <span class="badge bg-danger">Inativo</span> }
  @default { <span class="badge bg-secondary">-</span> }
}
```

**Nunca** use `*ngIf`, `*ngFor`, `*ngSwitch` — use `@if`, `@for`, `@switch`.

---

## 5. Leitura de Signal no Template

Signals **são funções** — sempre invocar com `()` no template:

```html
<!-- CORRETO -->
<h2>{{ pageTitle() }}</h2>
<gems-table [data]="entidades()" [isLoading]="isLoading()" />

@if (isEdit()) {
  <button class="btn-save">Atualizar</button>
}

<!-- ERRADO — sem () -->
<h2>{{ pageTitle }}</h2>
```

---

## 6. Rotas com Lazy Loading (Obrigatório)

```typescript
// app.routes.ts
export const routes: Routes = [
  {
    path: '{modulo}',
    children: [
      {
        path: 'list',
        loadComponent: () => import('./features/{modulo}/list/{modulo}-list.component')
          .then(c => c.{Entidade}ListComponent)
      },
      {
        path: 'form',
        loadComponent: () => import('./features/{modulo}/form/{modulo}-form.component')
          .then(c => c.{Entidade}FormComponent)
      },
      {
        path: 'form/:id',
        loadComponent: () => import('./features/{modulo}/form/{modulo}-form.component')
          .then(c => c.{Entidade}FormComponent)
      },
      { path: '', redirectTo: 'list', pathMatch: 'full' }
    ]
  }
];
```

Sempre `loadComponent` com lazy loading — nunca imports estáticos de componentes de rota.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `NgModule` | `standalone: true` em todo componente |
| `@Input() valor: T` | `valor = input<T>()` |
| `@Output() evento = new EventEmitter<T>()` | `evento = output<T>()` |
| `constructor(private svc: Svc)` | `private svc = inject(Svc)` |
| `*ngIf="cond"` | `@if (cond) { }` |
| `*ngFor="let i of lista"` | `@for (i of lista; track i.id) { }` |
| `BehaviorSubject` para estado local do componente | `signal()` + `computed()` |

---

## 7. Resumo: O que NUNCA usar

| Padrão antigo | Substituto moderno |
| :--- | :--- |
| `@Input() valor: T` | `valor = input<T>()` |
| `@Output() evento = new EventEmitter<T>()` | `evento = output<T>()` |
| `constructor(private svc: Svc)` | `private svc = inject(Svc)` |
| `*ngIf="cond"` | `@if (cond) { }` |
| `*ngFor="let i of lista"` | `@for (i of lista; track i.id) { }` |
| `*ngSwitch` / `*ngSwitchCase` | `@switch` / `@case` |
| `NgModule` | `standalone: true` |
| `BehaviorSubject + async pipe` | Prefer `signal()` + `computed()` para estado local |
