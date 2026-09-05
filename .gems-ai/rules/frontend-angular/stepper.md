---
stack: frontend-angular
module: gems-sdk
severity: mandatory
see-also: [signals-standards.md, stores.md, gems-components.md, feedback-alerts.md]
---

# Stepper / Wizard — Angular / gems-sdk

> **TL;DR:**
> - Arquitetura: Orquestrador + `StateService` (`signal()`) + `Config` (`steps[]`) + Steps isolados.
> - `StateService`: `signal()` para `currentStep`/`isReadOnly`. Métodos: `updateState`, `setStep`, `setReadOnly`, `clearState`. **Nunca** `BehaviorSubject`.
> - Orquestrador: `firstValueFrom()` em `ngOnInit` para dados existentes. Detecta `view/:id` → `isReadOnly`.
> - Steps: leem do `stateService.snapshot`, escrevem via `updateState()` — **nunca** chamam a Store diretamente.
> - Step-Resumo: chama o backend, chama `clearState()` após sucesso.
> - Rotas: `form`, `form/:id`, `view/:id` carregam o **mesmo** componente orquestrador.

## 1. Estrutura de Arquivos

```
features/{modulo}/form/
├── {modulo}-form.component.ts        # Orquestrador
├── {modulo}-form.component.html
├── {modulo}-state.service.ts          # State Service
├── {modulo}.config.ts                 # Configuração dos steps
└── steps/
    ├── step-dados/
    │   ├── step-dados.component.ts
    │   └── step-dados.component.html
    ├── step-detalhes/
    │   └── ...
    └── step-resumo/
        ├── step-resumo.component.ts
        └── step-resumo.component.html
```

---

## 2. Config dos Steps

```typescript
// {modulo}.config.ts
export const {MODULO}_FORM_STEPS = [
  { label: 'Dados Básicos', id: 1 },
  { label: 'Detalhes',      id: 2 },
  { label: 'Resumo',        id: 3 }
];
```

---

## 3. State Service

```typescript
import { Injectable, signal, computed } from '@angular/core';

export interface {Entidade}State {
  id?: string;
  campo1?: string;
  campo2?: string;
  currentStep: number;
  isReadOnly: boolean;
}

const INITIAL_STATE: {Entidade}State = {
  currentStep: 1,
  isReadOnly: false
};

@Injectable({ providedIn: 'root' })
export class {Entidade}StateService {

  private _state = signal<{Entidade}State>(INITIAL_STATE);

  // Leitura via computed (readonly)
  state = this._state.asReadonly();
  currentStep = computed(() => this._state().currentStep);
  isReadOnly = computed(() => this._state().isReadOnly);

  get snapshot(): {Entidade}State {
    return this._state();
  }

  updateState(partial: Partial<{Entidade}State>): void {
    this._state.update(s => ({ ...s, ...partial }));
  }

  setStep(step: number): void {
    this.updateState({ currentStep: step });
  }

  setReadOnly(isReadOnly: boolean): void {
    this.updateState({ isReadOnly });
  }

  clearState(): void {
    this._state.set(INITIAL_STATE);
  }
}
```

---

## 4. Componente Orquestrador

```typescript
import { Component, OnInit, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AlertService, NavigationService } from 'gems-sdk';
import { {Entidade}StateService } from './{modulo}-state.service';
import { {MODULO}_FORM_STEPS } from './{modulo}.config';
import { Step{Nome1}Component } from './steps/step-{nome1}/step-{nome1}.component';
import { StepResumoComponent } from './steps/step-resumo/step-resumo.component';

@Component({
  selector: 'app-{entidade}-form',
  standalone: true,
  imports: [CommonModule, RouterModule, Step{Nome1}Component, StepResumoComponent],
  templateUrl: './{entidade}-form.component.html'
})
export class {Entidade}FormComponent implements OnInit {

  private stateService = inject({Entidade}StateService);
  private store = inject({Entidade}Store);
  private alertService = inject(AlertService);
  private nav = inject(NavigationService);
  private route = inject(ActivatedRoute);

  steps = {MODULO}_FORM_STEPS;
  isLoading = signal(false);

  // Signals derivados do StateService
  currentStep = this.stateService.currentStep;
  isReadOnly = this.stateService.isReadOnly;

  progressPercentage = computed(() =>
    ((this.currentStep() - 1) / (this.steps.length - 1)) * 100
  );

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    const isViewRoute = this.route.snapshot.routeConfig?.path?.includes('view');

    this.stateService.setReadOnly(isViewRoute ?? false);

    if (id) {
      this.loadExisting(id);
    } else {
      this.stateService.clearState();
    }
  }

  async loadExisting(id: string): Promise<void> {
    this.isLoading.set(true);
    try {
      const data = await firstValueFrom(this.store.getById(id));
      this.stateService.updateState({
        id: data.id,
        campo1: data.campo1
        // ...mapear campos do modelo para o estado
      });

      if (this.isReadOnly()) {
        this.stateService.setStep(this.steps.length);  // Vai direto para o resumo
      }
      this.isLoading.set(false);
    } catch (err: any) {
      this.alertService.errorFromApi(err);
      this.nav.navigate(['/{modulo}/list']);
      this.isLoading.set(false);
    }
  }

  async cancelar(): Promise<void> {
    const confirm = await this.alertService.confirm('Cancelar', 'Os dados não salvos serão perdidos.');
    if (confirm.isConfirmed) {
      this.stateService.clearState();
      this.nav.navigate(['/{modulo}/list']);
    }
  }

  goBack(): void {
    if (this.isReadOnly()) {
      this.nav.navigate(['/{modulo}/list']);
      return;
    }
    if (this.currentStep() > 1) {
      this.stateService.setStep(this.currentStep() - 1);
    } else {
      this.cancelar();
    }
  }
}
```

---

## 5. Template do Orquestrador

```html
<div class="form-container animate__animated animate__fadeIn">

  <!-- HEADER -->
  <div class="header">
    <div style="display: flex; align-items: center; gap: 1rem;">
      <button class="btn-back" (click)="goBack()">
        <i class="fa-solid fa-arrow-left"></i>
      </button>
      <div class="header-titles">
        <h2>{Título do Módulo}</h2>
        <p class="subtitle">Siga os passos para preencher os dados.</p>
      </div>
    </div>
    @if (!isReadOnly()) {
      <button class="btn btn-danger" (click)="cancelar()">
        <i class="fa-solid fa-xmark"></i> Cancelar
      </button>
    }
  </div>

  <!-- STEPPER VISUAL (oculto em modo view) -->
  @if (!isReadOnly()) {
    <div class="stepper-container mb-4">
      <div class="stepper-wrapper">
        <div class="stepper-progress-bar-bg"></div>
        <div class="stepper-progress-bar-fill" [style.width.%]="progressPercentage()"></div>

        @for (step of steps; track step.id) {
          <div class="stepper-item"
               [class.active]="currentStep() === step.id"
               [class.completed]="currentStep() > step.id">
            <div class="step-circle">
              @if (currentStep() > step.id) {
                <i class="fa-solid fa-check"></i>
              } @else {
                <span>{{ step.id }}</span>
              }
            </div>
            <div class="step-label">{{ step.label }}</div>
          </div>
        }
      </div>
    </div>
  }

  <!-- STEP CONTENT -->
  @if (isLoading()) {
    <div class="loading-state">
      <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando...
    </div>
  } @else {
    <div class="step-content">
      @if (currentStep() === 1) { <app-step-{nome1} /> }
      @if (currentStep() === 2) { <app-step-{nome2} /> }
      @if (currentStep() === 3) { <app-step-resumo /> }
    </div>
  }

</div>
```

---

## 6. Componente de Step Individual

```typescript
@Component({
  selector: 'app-step-{nome}',
  standalone: true,
  imports: [CommonModule, FormsModule, FormCardComponent, InputMaskComponent],
  templateUrl: './step-{nome}.component.html'
})
export class Step{Nome}Component implements OnInit {

  private stateService = inject({Entidade}StateService);
  private alertService = inject(AlertService);

  // Lê estado atual
  state = this.stateService.state;
  isReadOnly = this.stateService.isReadOnly;

  // Dados locais do step
  campo1 = signal('');
  campo2 = signal('');

  ngOnInit(): void {
    const s = this.stateService.snapshot;
    this.campo1.set(s.campo1 ?? '');
  }

  next(): void {
    if (!this.campo1()) {
      this.alertService.warning('Atenção', 'Preencha todos os campos obrigatórios.');
      return;
    }
    this.stateService.updateState({ campo1: this.campo1() });
    this.stateService.setStep(N + 1);
  }

  goBack(): void {
    this.stateService.setStep(N - 1);
  }
}
```

```html
<gems-form-card title="{Título do Step}">
  <p form-card-subtitle>{Descrição do que o usuário faz neste passo}.</p>

  <div class="form-grid">
    <!-- Campos do step com [(ngModel)] ou formControlName -->
  </div>

  <div form-card-footer class="footer-step">
    <button type="button" class="btn-cancel" (click)="goBack()"
            [style.visibility]="isPrimeiro ? 'hidden' : 'visible'">
      <i class="fa-solid fa-arrow-left"></i> Voltar
    </button>
    <button type="button" class="btn-save" (click)="next()">
      Avançar <i class="fa-solid fa-arrow-right"></i>
    </button>
  </div>
</gems-form-card>
```

---

## 7. Step de Resumo

```html
<div class="animate__animated animate__fadeIn">
  <gems-form-card [title]="isReadOnly() ? 'Detalhes do Cadastro' : 'Resumo do Cadastro'">
    @if (!isReadOnly()) {
      <p form-card-subtitle>Verifique as informações antes de confirmar.</p>
    }

    <div class="resumo-sections">
      <gems-summary-card title="Seção 1" icon="fa-solid fa-user">
        <div details class="resumo-details-grid">
          <div class="detail-item">
            <span class="detail-label">CAMPO:</span>
            <span class="detail-value">{{ state().campo1 || '-' }}</span>
          </div>
        </div>
      </gems-summary-card>
    </div>

    <div form-card-footer class="footer-step">
      <button type="button" class="btn-cancel" (click)="goBack()">
        <i class="fa-solid" [class.fa-arrow-left]="!isReadOnly()" [class.fa-list]="isReadOnly()"></i>
        {{ isReadOnly() ? 'Voltar para Listagem' : 'Voltar' }}
      </button>
      @if (!isReadOnly()) {
        <button type="button" class="btn-save" (click)="save()" [disabled]="isLoading()">
          @if (isLoading()) {
            <i class="fa-solid fa-circle-notch fa-spin"></i> Processando...
          } @else {
            Confirmar <i class="fa-solid fa-check"></i>
          }
        </button>
      }
    </div>
  </gems-form-card>
</div>
```

### CSS obrigatório no componente de resumo:
```css
.resumo-sections { display: flex; flex-direction: column; gap: 1.5rem; margin-bottom: 1.5rem; }
.resumo-details-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 1.25rem; }
.detail-item { display: flex; flex-direction: column; gap: 0.25rem; }
.detail-label { font-size: 0.75rem; font-weight: 700; text-transform: uppercase; color: var(--gems-gray-600, #6c757d); }
.detail-value { font-size: 0.95rem; font-weight: 500; }
```

---

## 8. Rotas do Módulo Stepper

```typescript
export const routes: Routes = [
  { path: 'list',    loadComponent: () => import('./list/{modulo}-list.component').then(c => c.{Entidade}ListComponent) },
  { path: 'form',    loadComponent: () => import('./form/{modulo}-form.component').then(c => c.{Entidade}FormComponent) },
  { path: 'form/:id',loadComponent: () => import('./form/{modulo}-form.component').then(c => c.{Entidade}FormComponent) },
  { path: 'view/:id',loadComponent: () => import('./form/{modulo}-form.component').then(c => c.{Entidade}FormComponent) },
  { path: '',         redirectTo: 'list', pathMatch: 'full' }
];

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `BehaviorSubject` no `StateService` | `signal()` + `asReadonly()` para estado reativo |
| Step individual chamando `store.getById()` | Orquestrador carrega via `firstValueFrom()` — steps leem do `StateService` |
| `this.stateService._state = { ... }` (mutação direta) | `this.stateService.updateState({ campo })` |
| Não chamar `clearState()` após salvar no step-resumo | `stateService.clearState()` **obrigatório** após sucesso |
| Rotas `form-create` e `form-edit` separadas | Mesma rota `form` e `form/:id` → mesmo componente detecta `id` no `ngOnInit` |
```
