---
stack: frontend-angular
module: gems-sdk
severity: mandatory
see-also: [gems-components.md, feedback-alerts.md]
origem: local
precedencia: local-vence
versao: 2.0.0
---

# Padrões Visuais — Angular / gems-sdk

> **TL;DR:**
> - `provideGemsTheme()` em `app.config.ts` — obrigatório, sem exceção.
> - **NUNCA** hex hardcoded nos estilos. Sempre `var(--gems-primary-500)`, `var(--gems-bg-subtle)`, etc.
> - Confira a tabela da §2 antes de usar uma variável: `--gems-gray-*`, `--gems-bg-page` e
>   `--gems-text-primary` **não existem** na SDK 1.1.0.
> - Botões: `btn-save` / `btn-cancel` / `btn-novo` / `btn-back` / `btn-danger` — classes globais da SDK.
> - Ícones: **Font Awesome 6 Solid** (`fa-solid fa-{nome}`) — nunca `fa-regular`, `fa-light` ou Material Icons.
> - Layout: `list-container` / `form-container` + `animate__animated animate__fadeIn`.
> - Skeleton: `@if (isLoading()) { fa-circle-notch fa-spin }` — **nunca** spinner de terceiros.

## 1. provideGemsTheme (Obrigatório)

Configure uma única vez em `app.config.ts`:

```typescript
// app.config.ts
import { provideGemsTheme } from '@gabriel-mdias/angular-gems-sdk';

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

- Sobrescreve as variáveis CSS `--gems-*` globalmente com as cores do projeto.
- **Proibido** sobrescrever via `:root { --gems-*: #... }` nos estilos de componentes.

---

## 2. Variáveis CSS — Como Usar

Sempre use variáveis da SDK. **Nunca** valores hex hardcoded.

```css
/* CORRETO */
color:            var(--gems-primary-500);
background-color: var(--gems-secondary-100);
border-color:     var(--gems-primary-300);
color:            var(--gems-text-muted);
background:       var(--gems-bg-subtle);

/* ERRADO — hex hardcoded */
color:            #1e293b;
background-color: #3b82f6;

/* ERRADO — variáveis que NÃO EXISTEM na SDK 1.1.0.
   Não falham: resolvem para vazio e o navegador ignora a declaração, então o
   elemento simplesmente herda a cor errada, sem nenhum aviso no console. */
color:            var(--gems-text-primary);
background:       var(--gems-bg-page);
border-color:     var(--gems-gray-200);
```

**Paleta completa disponível:**
| Grupo | Variável | Uso típico |
| :--- | :--- | :--- |
| Primary | `--gems-primary-{50,100,200,300,400,500,600,700,800,900}` | Cor principal, headers |
| Secondary | `--gems-secondary-{50..900}` | Cor de destaque, links |
| Tertiary | `--gems-tertiary-{50..900}` | Cor de apoio, hover |
| Danger | `--gems-danger-{50..900}` | Erro, exclusão, validação |
| Success | `--gems-success-{50..900}` | Confirmação, estado positivo |
| Warning | `--gems-warning-{50..900}` | Alerta |
| Info | `--gems-info-{50..900}` | Informação |
| BG | `--gems-bg`, `--gems-bg-subtle`, `--gems-bg-muted`, `--gems-bg-emphasis`, `--gems-bg-inverse` | Fundos de página, card, divisórias |
| Text | `--gems-text-main`, `--gems-text-muted` | Hierarquia textual |

Cada uma das sete paletas de cor (`primary`, `secondary`, `tertiary`, `danger`, `success`,
`warning`, `info`) emite, além da escala `50..900`, seis variáveis de estado:

| Sufixo | Valor |
| :--- | :--- |
| `--gems-{cor}` | o hex exato informado em `provideGemsTheme()`, sem ajuste de lightness |
| `--gems-{cor}-hover` | lightness +8 |
| `--gems-{cor}-active` | lightness -5 |
| `--gems-{cor}-disabled` | saturação -20, lightness +20 |
| `--gems-{cor}-focus-ring` | a cor com alpha 0.3, para anel de foco |
| `--gems-{cor}-contrast` | `#111827` ou `#ffffff`, o que contrastar com a cor |

> **Esta lista foi conferida contra a SDK 1.1.0 instalada.** Versões anteriores deste arquivo
> documentavam `--gems-gray-*`, `--gems-bg-page`, `--gems-bg-surface`, `--gems-bg-elevated`,
> `--gems-text-primary` e `--gems-text-secondary`, que **nunca existiram**. Só há dois níveis de
> texto (`main` e `muted`) e cinco de fundo — para bordas e divisórias use `--gems-bg-muted`.
>
> ⚠️ **A SDK Angular 2.0.0 já foi publicada, e esta lista ainda descreve a 1.1.0.** O projeto
> declara `^1.1.0`; a subida é da rodada `009` (bloco E10). **Quem fizer a subida revisa esta
> seção contra o `gems-global.css` da 2.0.0 na mesma alteração** — uma variável que deixe de
> existir não falha: ela resolve para vazio, o navegador ignora a declaração e o elemento herda a
> cor errada, sem nada no console. Registrado na rodada `008` (bloco M, item A-9 da `medicao-m01.md`).

---

## 3. Botões

Use exclusivamente as classes globais da SDK. Não crie estilos `button` ad-hoc.

| Classe | Uso | Ícone padrão |
| :--- | :--- | :--- |
| `btn-save` | Ação principal (Salvar/Confirmar/Avançar) | `fa-floppy-disk` / `fa-arrow-right` |
| `btn-cancel` | Ação secundária (Cancelar/Voltar/Limpar) | `fa-xmark` / `fa-arrow-left` / `fa-eraser` |
| `btn-novo` | Criar registro na listagem | `fa-plus` |
| `btn-back` | Voltar para listagem (ícone só, sem texto) | `fa-arrow-left` |
| `btn-danger` | Ação destrutiva (quando fora da tabela) | `fa-trash` |
| `btn btn-info` | Ação de visualização (via `GemsTableAction`) | `fa-eye` |
| `btn btn-primary` | Ação de edição (via `GemsTableAction`) | `fa-pen` |
| `btn btn-danger` | Ação de exclusão (via `GemsTableAction`) | `fa-trash` |

**Ícone sempre à direita** em `btn-save`; **à esquerda** em `btn-cancel`.

```html
<!-- Exemplos corretos -->
<button class="btn-save" (click)="save()">
  Salvar <i class="fa-solid fa-floppy-disk"></i>
</button>

<button class="btn-cancel" routerLink="/{modulo}/list">
  <i class="fa-solid fa-arrow-left"></i> Voltar
</button>

<button class="btn-novo" routerLink="/{modulo}/form">
  <i class="fa-solid fa-plus"></i> Nova {Entidade}
</button>
```

---

## 4. Ícones

Use exclusivamente **Font Awesome 6 Solid** (`fa-solid`).

```html
<i class="fa-solid fa-plus"></i>
<i class="fa-solid fa-eye"></i>
<i class="fa-solid fa-pen"></i>
<i class="fa-solid fa-trash"></i>
<i class="fa-solid fa-floppy-disk"></i>
<i class="fa-solid fa-xmark"></i>
<i class="fa-solid fa-arrow-left"></i>
<i class="fa-solid fa-arrow-right"></i>
<i class="fa-solid fa-search"></i>
<i class="fa-solid fa-eraser"></i>
<i class="fa-solid fa-circle-notch fa-spin"></i>   <!-- loading -->
<i class="fa-solid fa-check"></i>
<i class="fa-solid fa-list"></i>
```

**Proibido:** `fa-regular`, `fa-light`, Material Icons, ngx-icons ou qualquer outra biblioteca.

---

## 5. Layout e Grid

### Containers de página:
```html
<!-- Listagem -->
<div class="list-container animate__animated animate__fadeIn">...</div>

<!-- Formulário/Stepper -->
<div class="form-container animate__animated animate__fadeIn">...</div>
```

### Grid de campos (dentro de `gems-form-card`):
```html
<div class="form-grid">
  <div class="form-group">...</div>           <!-- 50% de largura -->
  <div class="form-group full-width">...</div> <!-- 100% de largura -->
</div>
```

### Labels:
```html
<label class="fw-bold" for="campo">Campo Opcional</label>
<label class="required fw-bold" for="campo">Campo Obrigatório</label>
<!-- A classe `required` adiciona asterisco vermelho via CSS global -->
```

---

## 6. Micro-animações

- Todo wrapper de rota de componente usa `animate__animated animate__fadeIn` (da biblioteca `animate.css`, já incluída na SDK).
- **Não** adicione outras animações customizadas via `@keyframes`.
- Transições de step do stepper são gerenciadas internamente pelo orquestrador; não estilize.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `color: #1e293b` no CSS | `color: var(--gems-primary-900)` |
| `:root { --gems-primary-500: red }` em CSS de componente | `provideGemsTheme({ primary: '#...' })` em `app.config.ts` |
| `ngx-spinner` ou spinner customizado com `border + @keyframes` | `@if (isLoading()) { <i class="fa-solid fa-circle-notch fa-spin"> }` |
| `<i class="fa-regular fa-eye">` | `<i class="fa-solid fa-eye">` — apenas Font Awesome 6 **Solid** |
| Botão com estilo inline: `style="background: var(--gems-primary)"` | Classe SDK: `btn-save`, `btn-cancel`, `btn-novo`, `btn-back`, `btn-danger` |

```html
<!-- Correto: fadeIn no wrapper externo do componente -->
<div class="list-container animate__animated animate__fadeIn">
  ...
</div>
```

---

## 7. Loading / Skeleton

Sempre use o padrão signal + `@if`:

```html
@if (isLoading()) {
  <div class="loading-state">
    <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando...
  </div>
} @else {
  <!-- conteúdo real -->
}
```

- **Proibido:** spinners de terceiros, `ngx-spinner`, ou CSS `::before` com `border` girando.
- O estado `isLoading` é sempre um `signal<boolean>(false)` no componente.

---

## 8. Header de Página

Estrutura obrigatória nas telas de listagem e formulário:

```html
<!-- Listagem -->
<div class="header">
  <div class="header-titles">
    <h2>{Título do Módulo}</h2>
    <p class="subtitle">Gerencie os(as) {entidades}.</p>
  </div>
  <button class="btn-novo" routerLink="/{modulo}/form">
    <i class="fa-solid fa-plus"></i> Nova {Entidade}
  </button>
</div>

<!-- Formulário/Stepper -->
<div class="header">
  <div style="display: flex; align-items: center; gap: 1rem;">
    <button class="btn-back" routerLink="/{modulo}/list">
      <i class="fa-solid fa-arrow-left"></i>
    </button>
    <div class="header-titles">
      <h2>{{ pageTitle() }}</h2>
      <p class="subtitle">{{ pageSubtitle() }}</p>
    </div>
  </div>
  @if (!isReadOnly()) {
    <button class="btn btn-danger" (click)="cancelar()">
      <i class="fa-solid fa-xmark"></i> Cancelar
    </button>
  }
</div>
```
