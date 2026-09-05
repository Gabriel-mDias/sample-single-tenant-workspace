---
stack: frontend-angular
module: gems-sdk
severity: mandatory
see-also: [signals-standards.md, stores.md, forms.md, lists.md, stepper.md]
---

# Organização de Módulos & Estrutura de Feature — Angular

> **TL;DR:**
> - Estrutura por feature em `src/app/features/{modulo}/`: `models/`, `services/`, `list/`, `form/steps/`.
> - CRUD simples: `list/` + `form/`. Stepper/Wizard: `list/` + `form/steps/` + `{modulo}-state.service.ts`.
> - `NavigationService` para toda navegação programática — **nunca** `Router` diretamente nos componentes.
> - Responsividade: Bootstrap grid (`col-md-6`) dentro dos cards SDK. `gems-table` scroll horizontal automático em mobile.

## 1. Estrutura de Pastas por Feature

```
src/app/features/{modulo}/
├── models/
│   ├── {modulo}.model.ts               # Interfaces TypeScript da entidade
│   └── {modulo}-filter.model.ts        # Interface de filtro (campos opcionais)
├── services/
│   └── {modulo}.store.ts               # Estende GemsBaseStore (gems-sdk)
├── list/
│   ├── {modulo}-list.component.ts
│   ├── {modulo}-list.component.html
│   └── {modulo}-list.component.css     # Geralmente vazio — estilos globais
└── form/
    ├── {modulo}-form.component.ts
    ├── {modulo}-form.component.html
    ├── {modulo}-form.component.css
    ├── {modulo}-state.service.ts        # Apenas em Stepper — Signal + BehaviorSubject de estado
    ├── {modulo}.config.ts               # Apenas em Stepper — lista de steps
    └── steps/                           # Apenas em Stepper
        ├── step-{nome}/
        │   ├── step-{nome}.component.ts
        │   └── step-{nome}.component.html
        └── step-resumo/
            ├── step-resumo.component.ts
            └── step-resumo.component.html
```

---

## 2. Interfaces TypeScript

```typescript
// {modulo}.model.ts
export interface {Entidade}Model {
  id?: string;          // Sempre string no frontend (não UUID)
  dataCriacao?: string;
  campoObrigatorio: string;
  campoOpcional?: string;
}

// {modulo}-filter.model.ts
export interface {Entidade}FilterModel {
  textoLivre?: string;
  situacao?: string;
  dataMin?: string;
  dataMax?: string;
}
```

### Regras de interface:
- IDs sempre `string` (não `UUID`) — conversão ocorre no backend.
- Campos opcionais com `?` — jamais `null` explícito em interfaces.
- Interfaces de filtro com todos os campos opcionais (`?`).

---

## 3. NavigationService

Para toda navegação programática, use o `NavigationService` da SDK — nunca o `Router` diretamente nos componentes:

```typescript
export class {Entidade}ListComponent {
  private navigationService = inject(NavigationService);

  navegarParaForm() {
    this.navigationService.navigate(['/{modulo}/form']);
  }

  navegarParaEdicao(id: string) {
    this.navigationService.navigate(['/{modulo}/form', id]);
  }

  navegarParaVisualizacao(id: string) {
    this.navigationService.navigate(['/{modulo}/view', id]);
  }
}
```

---

## 4. Padrão de Ações e Botões

| Ação | Classe CSS | Ícone | Posição do ícone |
| :--- | :--- | :--- | :--- |
| Novo registro | `.btn-novo` | `fa-plus` | **Antes** do texto |
| Salvar / Confirmar | `.btn-save` | `fa-check` / `fa-floppy-disk` | **Depois** do texto |
| Pesquisar | `.btn-save` | `fa-search` | **Depois** do texto |
| Cancelar / Limpar | `.btn-cancel` | `fa-xmark` / `fa-eraser` | **Depois** do texto |
| Voltar | `.btn-back` | `fa-arrow-left` | Sozinho (sem texto) |
| Avançar (stepper) | `.btn-save` | `fa-arrow-right` | **Depois** do texto |
| Voltar (stepper) | `.btn-cancel` | `fa-arrow-left` | **Antes** do texto |
| Excluir | `.btn-danger` | `fa-trash` | **Depois** do texto |

---

## 5. Responsividade

- Bootstrap grid (`row`, `col-md-6`, `col-md-4`) dentro dos cards SDK.
- `gems-table` com scroll horizontal automático em telas pequenas.
- `gems-form-card` ocupa largura total em mobile.
- Formulários com `form-grid` (CSS Grid customizado da SDK) ou Bootstrap rows.

---

## 6. Convenções de Nomenclatura de Arquivo

| Tipo | Padrão | Exemplo |
| :--- | :--- | :--- |
| Componente | `{modulo}-{tipo}.component.ts` | `produto-form.component.ts` |
| Store | `{modulo}.store.ts` | `produto.store.ts` |
| Model | `{modulo}.model.ts` | `produto.model.ts` |
| Filter | `{modulo}-filter.model.ts` | `produto-filter.model.ts` |
| State Service | `{modulo}-state.service.ts` | `produto-state.service.ts` |
| Config | `{modulo}.config.ts` | `produto.config.ts` |
| Step | `step-{nome}.component.ts` | `step-dados.component.ts` |

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `this.router.navigate([...])` diretamente no componente | `this.navigationService.navigate([...])` |
| `id?: number` na interface de model | `id?: string` — IDs sempre `string` no frontend |
| `campo: null` em interface TypeScript | `campo?: string` — usar opcional `?`, nunca `null` explícito |
| Feature sem pasta `models/` e `services/` | Estrutura obrigatória: `models/`, `services/`, `list/`, `form/` |
