---
stack: frontend-angular
module: gems-sdk
severity: mandatory
see-also: [visual-standards.md, feedback-alerts.md, forms.md, lists.md]
---

# Catálogo de Componentes — Angular GEMS SDK

> **TL;DR:**
> - `gems-table`: listagens paginadas. Inputs: `columns/data/actions/totalRecords/page/size/sortField/sortDirection`. Outputs: `pageChange/actionClick`.
> - `gems-form-card`: container de formulários e filtros. Slots: `form-card-subtitle` / `form-card-footer`.
> - `gems-summary-card`: resumo expandível — **apenas** em step-resumo e telas `view`. Slots: `summary` / `details`.
> - `gems-card-list-select`: seleção visual em cards. Two-way: `[(selectedId)]` ou `[(selectedIds)]`.
> - Inputs mascarados: `gems-input-mask` (`cep/telefone/moeda/rg/email`), `gems-input-data` (datas), `gems-input-documento` (CPF/CNPJ).
> - Theming: `provideGemsTheme()` em `app.config.ts` + `var(--gems-primary-500)` — **NUNCA** hex hardcoded.

## 1. gems-table

Tabela inteligente com paginação, ordenação e ações.

```html
<gems-table
  [columns]="columns"
  [data]="entidades()"
  [actions]="actions"
  [totalRecords]="totalRecords()"
  [page]="pageable.page ?? 0"
  [size]="pageable.size ?? 10"
  [sortField]="currentSortField"
  [sortDirection]="currentSortDirection"
  emptyMessage="Nenhum item encontrado."
  (actionClick)="handleAction($event)"
  (pageChange)="onPageChange($event)">
</gems-table>
```

**TableColumn:**
```typescript
{ field: 'nome',    header: 'Nome',      sortable: true }
{ field: 'valor',   header: 'Valor',     sortable: true, sortParam: 'valor' }
{ field: 'doc',     header: 'Documento', type: 'document', docTypeField: 'tipoDocumento' }
{ field: 'actions', header: '',          type: 'actions',  sortable: false }
```

**TableAction:**
```typescript
{ actionName: 'view',   icon: 'fa-solid fa-eye',   tooltip: 'Visualizar', colorClass: 'btn-info' }
{ actionName: 'edit',   icon: 'fa-solid fa-pen',   tooltip: 'Editar',     colorClass: 'btn-primary' }
{ actionName: 'delete', icon: 'fa-solid fa-trash', tooltip: 'Excluir',    colorClass: 'btn-danger' }
```

**Outputs:**
- `(pageChange)` → emite `Pageable` (`{ page, size, sort }`)
- `(actionClick)` → emite `{ action: string; row: any }`

---

## 2. gems-form-card

Container de formulários, filtros e passos de stepper.

```html
<gems-form-card title="Dados Gerais">
  <p form-card-subtitle>Subtítulo descritivo.</p>

  <!-- Conteúdo do card (projeção de conteúdo padrão) -->
  <div class="form-grid"> ... </div>

  <div form-card-footer>
    <button class="btn-cancel">Cancelar</button>
    <button class="btn-save">Salvar</button>
  </div>
</gems-form-card>
```

**Slots:**
- `form-card-subtitle` → texto de orientação abaixo do título.
- `form-card-footer` → rodapé com botões de ação.

---

## 3. gems-summary-card

Card expansível para exibição de dados (somente leitura). Use **apenas** no step de resumo e em telas de visualização.

```html
<gems-summary-card title="Dados Pessoais" icon="fa-solid fa-user" [expanded]="true">
  <div summary class="summary-preview">
    <span>{{ nome }}</span>
  </div>
  <div details class="resumo-details-grid">
    <div class="detail-item">
      <span class="detail-label">NOME:</span>
      <span class="detail-value">{{ nome }}</span>
    </div>
  </div>
</gems-summary-card>
```

**Slots:**
- `summary` → preview quando recolhido.
- `details` → conteúdo expandido.

---

## 4. gems-card-list-select

Seleção visual de registros em formato card (stepper de seleção).

```html
<gems-card-list-select
  [items]="itens"
  [(selectedIds)]="selectedIdsArray"
  [multiple]="true"
  icon="fa-solid fa-graduation-cap"
  titleKey="nome"
  subtitleKey="descricao"
  idKey="id"
  [pageSize]="6"
  [isReadOnly]="isReadOnly()">
</gems-card-list-select>
```

Para seleção única: `[(selectedId)]="selectedId"` (sem array).

---

## 5. Inputs Mascarados

Todos implementam `ControlValueAccessor` — compatíveis com `formControlName` e `[(ngModel)]`.

### gems-input-text
```html
<gems-input-text formControlName="nome" label="Nome" [required]="true"
  icon="fa-solid fa-user" placeholder="Digite o nome">
</gems-input-text>
```

### gems-input-mask
```html
<!-- maskType: 'cep' | 'telefone' | 'moeda' | 'rg' | 'email' -->
<gems-input-mask formControlName="telefone" label="Telefone"
  maskType="telefone" icon="fa-solid fa-phone" placeholder="(00) 00000-0000">
</gems-input-mask>
```

### gems-input-data
```html
<!-- inputFormat: 'diaMesAno' | 'mesAno' | 'ano' | 'fullData' -->
<gems-input-data formControlName="dataNascimento" label="Data de Nascimento"
  inputFormat="diaMesAno" placeholder="DD/MM/AAAA">
</gems-input-data>
```

### gems-input-documento
```html
<!-- Alterna CPF ↔ CNPJ automaticamente -->
<gems-input-documento
  [numeroDocumento]="form.get('numeroDocumento')?.value"
  (numeroDocumentoChange)="form.get('numeroDocumento')?.setValue($event)"
  [tipoDocumento]="form.get('tipoDocumento')?.value"
  (tipoDocumentoChange)="form.get('tipoDocumento')?.setValue($event)">
</gems-input-documento>
```

### gems-input-checkbox
```html
<gems-input-checkbox formControlName="ativo" label="Ativo" [isSwitch]="true"
  topLabel="Status">
</gems-input-checkbox>
```

### gems-field-error
```html
<!-- Exibe erros de BusinessException abaixo de um campo -->
<gems-field-error [control]="form.get('campo')">
</gems-field-error>
```

---

## 6. Serviços

### GemsBaseStore
```typescript
export class {Entidade}Store extends GemsBaseStore {
  constructor() { super('api/{recurso}'); }

  // Métodos disponíveis (tipados com generics):
  // this.get<T>(path)
  // this.post<TReturn, TBody>(path, body, options?)
  // this.put<TReturn, TBody>(path, body)
  // this.delete<T>(path)
}
```

### AlertService
```typescript
private alertService = inject(AlertService);

// Uso:
this.alertService.success('Sucesso', 'Operação realizada com sucesso.');
this.alertService.warning('Atenção', 'Confira os dados antes de continuar.');
this.alertService.error('Erro', 'Ocorreu um problema inesperado.');
this.alertService.errorFromApi(err);    // Parse automático de BusinessException

const result = await this.alertService.confirm('Excluir', 'Tem certeza?');
if (result.isConfirmed) { /* ... */ }
```

### NavigationService
```typescript
private nav = inject(NavigationService);
this.nav.navigate(['/{modulo}/list']);
this.nav.navigate(['/{modulo}/form', id]);
```

---

## 7. Theming

Configure as cores do projeto via `provideGemsTheme()` em `app.config.ts`:

```typescript
// app.config.ts
import { provideGemsTheme } from 'gems-sdk';

export const appConfig: ApplicationConfig = {
  providers: [
    // ...
    provideGemsTheme({
      primary:    '#1e3a5f',
      secondary:  '#3b82f6',
      tertiary:   '#6366f1',
      background: '#f8fafc'
    })
  ]
};
```

Em estilos CSS, use variáveis do tema — **nunca** hex hardcoded:

```css
/* CORRETO */
color: var(--gems-primary-500);
background: var(--gems-secondary-100);
border-color: var(--gems-primary-300);

/* ERRADO */
color: #1e293b;
background: #3b82f6;
```

**Variáveis disponíveis:** `--gems-primary-{50..900}`, `--gems-secondary-{50..900}`, `--gems-tertiary-{50..900}`, `--gems-bg-*`, `--gems-text-*`, `--gems-gray-{50..900}`.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `gems-summary-card` em formulários de criação/edição | Apenas em step-resumo e telas `view/:id` |
| Clicar em linha da `gems-table` para navegar (sem `TableAction`) | `(actionClick)` com `TableAction` configurado para a ação |
| `new AlertService()` | `private alert = inject(AlertService)` |
| `provideGemsTheme()` com hex omitido | Sempre passar todas as cores do tema do projeto |
