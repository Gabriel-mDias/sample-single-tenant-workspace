---
stack: frontend-angular
module: gems-sdk, @angular/forms
severity: mandatory
see-also: [signals-standards.md, gems-components.md, feedback-alerts.md, stepper.md]
---

# Formulários Simples — Angular / gems-sdk

> **TL;DR:**
> - `standalone: true`. `FormGroup` com `FormBuilder` no **construtor** — nunca template-driven.
> - `inject()` para dependências — **nunca** `constructor` com parâmetros.
> - `ngOnInit`: detecta `id` na rota para modo edição (`isEdit.set(true)`).
> - Template: header (`btn-back`) + `[formGroup]` + `gems-form-card` + `form-card-footer` (Cancelar/Salvar).
> - `@if (isLoading())` para skeleton — **nunca** `*ngIf`.
> - Inputs formatados: `gems-input-mask`, `gems-input-data`, `gems-input-documento` — **NUNCA** `<input type="text">` nu.

## 1. Estrutura de Arquivos

```
features/{modulo}/form/
├── {modulo}-form.component.ts
├── {modulo}-form.component.html
└── {modulo}-form.component.css    # Geralmente vazio
```

---

## 2. Componente TypeScript

```typescript
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormCardComponent, AlertService, NavigationService, InputMaskComponent } from 'gems-sdk';
import { {Entidade}Store } from '../services/{entidade}.store';
import { {Entidade}Model } from '../../../models/{entidade}.model';

@Component({
  selector: 'app-{entidade}-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, FormCardComponent, InputMaskComponent],
  templateUrl: './{entidade}-form.component.html'
})
export class {Entidade}FormComponent implements OnInit {

  private store = inject({Entidade}Store);
  private route = inject(ActivatedRoute);
  private alertService = inject(AlertService);
  private navigationService = inject(NavigationService);
  private fb = inject(FormBuilder);

  // Signals de estado
  isLoading = signal(false);
  isEdit = signal(false);

  // Computed
  pageTitle = computed(() => this.isEdit() ? 'Editar {Entidade}' : 'Nova {Entidade}');
  pageSubtitle = computed(() => this.isEdit() ? 'Atualize os dados.' : 'Preencha os dados abaixo.');

  // FormGroup — construído no construtor, nunca no ngOnInit
  {entidade}Form: FormGroup;

  constructor() {
    this.{entidade}Form = this.fb.group({
      id: [null],
      dataCriacao: [null],
      campoObrigatorio: ['', Validators.required],
      campoOpcional: ['']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit.set(true);
      this.load{Entidade}(id);
    }
  }

  load{Entidade}(id: string): void {
    this.isLoading.set(true);
    this.store.getById(id).subscribe({
      next: (data: {Entidade}Model) => {
        this.{entidade}Form.patchValue(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      }
    });
  }

  save(): void {
    if (this.{entidade}Form.invalid) return;
    this.isLoading.set(true);
    const model: {Entidade}Model = this.{entidade}Form.value;

    const call$ = this.isEdit() && model.id
      ? this.store.update(model.id, model)
      : this.store.create(model);

    call$.subscribe({
      next: () => {
        this.alertService.success('Sucesso', `{Entidade} ${this.isEdit() ? 'atualizado(a)' : 'criado(a)'} com sucesso.`);
        this.navigationService.navigate(['/{modulo}/list']);
      },
      error: (err) => {
        this.alertService.errorFromApi(err);
        this.isLoading.set(false);
      }
    });
  }
}
```

---

## 3. Template HTML (Estrutura Obrigatória)

```html
<div class="form-container">

  <!-- HEADER -->
  <div class="header">
    <button class="btn-back" routerLink="/{modulo}/list">
      <i class="fa-solid fa-arrow-left"></i>
    </button>
    <div class="header-titles">
      <h2>{{ pageTitle() }}</h2>
      <p class="subtitle">{{ pageSubtitle() }}</p>
    </div>
  </div>

  <!-- LOADING -->
  @if (isLoading()) {
    <div class="loading-state">
      <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando dados...
    </div>
  } @else {
    <!-- FORMULÁRIO -->
    <form [formGroup]="{entidade}Form" (ngSubmit)="save()">
      <gems-form-card title="Dados Gerais">
        <p form-card-subtitle>Informações básicas de identificação.</p>

        <div class="form-grid">

          <div class="form-group full-width">
            <label for="campoObrigatorio" class="required fw-bold">Campo Obrigatório</label>
            <input type="text" id="campoObrigatorio" class="form-control"
                   formControlName="campoObrigatorio" placeholder="Digite...">
          </div>

          <div class="form-group">
            <gems-input-mask
              formControlName="telefone"
              label="Telefone"
              maskType="telefone"
              icon="fa-solid fa-phone"
              placeholder="(00) 00000-0000">
            </gems-input-mask>
          </div>

          <div class="form-group">
            <gems-input-data
              formControlName="dataNascimento"
              label="Data de Nascimento"
              inputFormat="diaMesAno"
              placeholder="DD/MM/AAAA">
            </gems-input-data>
          </div>

        </div>

        <div form-card-footer>
          <button type="button" class="btn-cancel" routerLink="/{modulo}/list">
            Cancelar <i class="fa-solid fa-xmark"></i>
          </button>
          <button type="submit" class="btn-save" [disabled]="{entidade}Form.invalid">
            @if (isEdit()) { Atualizar } @else { Salvar }
            <i class="fa-solid fa-floppy-disk"></i>
          </button>
        </div>
      </gems-form-card>
    </form>
  }

</div>
```

---

## 4. Regras do Template

### Header:
- `btn-back` com `routerLink` para a listagem.
- `<h2>` dinâmico via signal computed.

### Form:
- `[formGroup]` no `<form>`, nunca em elementos individuais.
- `(ngSubmit)` para o evento de submit.

### Grid de campos:
- `<div class="form-grid">` como container.
- `<div class="form-group">` = 50% da grid.
- `<div class="form-group full-width">` = 100%.
- `<label>` com `for` e classe `.fw-bold` em campos padrão.
- `<label class="required fw-bold">` em campos obrigatórios (asterisco via CSS global).

### Footer:
- Dentro de `<div form-card-footer>`.
- `btn-cancel` com `routerLink` para listagem.
- `btn-save` com `[disabled]` no form invalid.
- Texto dinâmico via `@if (isEdit())`.

---

## 5. Inputs Mascarados da SDK

**NUNCA** use `<input type="text">` para campos com máscara — use os componentes da SDK:

```html
<!-- E-mail -->
<gems-input-mask formControlName="email" label="E-mail" [required]="true"
  maskType="email" icon="fa-solid fa-envelope" placeholder="contato@email.com">
</gems-input-mask>

<!-- Telefone -->
<gems-input-mask formControlName="telefone" label="Telefone"
  maskType="telefone" icon="fa-solid fa-phone" placeholder="(00) 00000-0000">
</gems-input-mask>

<!-- CEP -->
<gems-input-mask formControlName="cep" label="CEP"
  maskType="cep" icon="fa-solid fa-location-dot" placeholder="00000-000">
</gems-input-mask>

<!-- Moeda -->
<gems-input-mask formControlName="valor" label="Valor"
  maskType="moeda" placeholder="R$ 0,00">
</gems-input-mask>

<!-- CPF / CNPJ (alterna automaticamente) -->
<gems-input-documento
  [numeroDocumento]="{entidade}Form.get('numeroDocumento')?.value"
  (numeroDocumentoChange)="{entidade}Form.get('numeroDocumento')?.setValue($event)"
  [tipoDocumento]="{entidade}Form.get('tipoDocumento')?.value"
  (tipoDocumentoChange)="{entidade}Form.get('tipoDocumento')?.setValue($event)">
</gems-input-documento>

<!-- Data -->
<gems-input-data formControlName="dataNascimento" label="Data de Nascimento"
  inputFormat="diaMesAno" placeholder="DD/MM/AAAA">
</gems-input-data>
```

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `this.{entidade}Form = this.fb.group({...})` no `ngOnInit` | `FormGroup` construído no **construtor** |
| `constructor(private fb: FormBuilder, private store: ...)` | `private fb = inject(FormBuilder)` + `private store = inject(...)` |
| `*ngIf="isLoading"` | `@if (isLoading())` — signals são funções |
| `<input type="text" placeholder="000.000.000-00">` para CPF | `<gems-input-documento>` — nunca input nativo para campos formatados |
| `<input type="text" placeholder="(00) 00000-0000">` para telefone | `<gems-input-mask maskType="telefone">` |
