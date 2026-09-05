---
stack: frontend-angular
module: jasmine, karma
severity: mandatory
see-also: [stores.md, forms.md, lists.md]
---

# Testes — Angular / gems-sdk

> **TL;DR:**
> - Framework: Jasmine + Karma (padrão Angular). Alternativa: Jest (substituir `karma.conf.js`).
> - Componentes: `TestBed` com `NO_ERRORS_SCHEMA` — isola do template (gems-table, gems-form-card etc.).
> - Mock: `jasmine.createSpyObj<T>()` tipado. **Nunca** `HttpClientTestingModule` em testes de componentes.
> - Padrão AAA: **Arrange** (setup) → **Act** (invocar) → **Assert** (verificar).
> - Nomenclatura: `deveFazerAlgo_quandoCondicao` (camelCase, português, sem "it should").
> - Signals: `fixture.detectChanges()` após setup dispara `ngOnInit` e propagação de signals.

## 1. Estrutura de Arquivo

```
features/{modulo}/
├── list/
│   ├── {modulo}-list.component.ts
│   └── {modulo}-list.component.spec.ts   ← mesmo diretório
└── form/
    ├── {modulo}-form.component.ts
    └── {modulo}-form.component.spec.ts
```

---

## 2. Teste de Componente de Listagem

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, throwError } from 'rxjs';
import { {Entidade}ListComponent } from './{entidade}-list.component';
import { {Entidade}Store } from '../services/{entidade}.store';
import { AlertService, NavigationService } from 'gems-sdk';

describe('{Entidade}ListComponent', () => {
  let fixture: ComponentFixture<{Entidade}ListComponent>;
  let component: {Entidade}ListComponent;
  let storeSpy: jasmine.SpyObj<{Entidade}Store>;
  let alertSpy: jasmine.SpyObj<AlertService>;
  let navSpy: jasmine.SpyObj<NavigationService>;

  const mockEntidades = [
    { id: '1', nome: 'Registro Teste' }
  ];

  beforeEach(async () => {
    storeSpy = jasmine.createSpyObj('{Entidade}Store', ['search', 'delete{Entidade}']);
    alertSpy = jasmine.createSpyObj('AlertService', ['success', 'errorFromApi', 'confirm']);
    navSpy   = jasmine.createSpyObj('NavigationService', ['navigate']);

    storeSpy.search.and.returnValue(of({ content: mockEntidades, totalElements: 1 }));

    await TestBed.configureTestingModule({
      imports: [{Entidade}ListComponent],
      providers: [
        { provide: {Entidade}Store, useValue: storeSpy },
        { provide: AlertService,      useValue: alertSpy },
        { provide: NavigationService, useValue: navSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture   = TestBed.createComponent({Entidade}ListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deveCarregarEntidades_quandoInicializar', () => {
    expect(storeSpy.search).toHaveBeenCalledTimes(1);
    expect(component.entidades()).toEqual(mockEntidades);
    expect(component.totalRecords()).toBe(1);
  });

  it('deveExibirLoading_quandoPesquisar', () => {
    storeSpy.search.and.returnValue(of({ content: [], totalElements: 0 }));
    component.search();
    expect(component.isLoading()).toBeFalse();
  });

  it('deveChamarErrorFromApi_quandoSearchFalhar', () => {
    storeSpy.search.and.returnValue(throwError(() => ({ status: 500 })));
    component.loadEntidades();
    expect(alertSpy.errorFromApi).toHaveBeenCalled();
  });

  it('deveNavegar_quandoEditarRegistro', () => {
    component.handleAction({ action: 'edit', row: { id: '1' } });
    expect(navSpy.navigate).toHaveBeenCalledWith(['/{modulo}/form', '1']);
  });

  it('deveExcluirERecarregar_quandoConfirmar', async () => {
    alertSpy.confirm.and.resolveTo({ isConfirmed: true });
    storeSpy.delete{Entidade}.and.returnValue(of(undefined));
    storeSpy.search.and.returnValue(of({ content: [], totalElements: 0 }));

    await component.remove('1');

    expect(storeSpy.delete{Entidade}).toHaveBeenCalledWith('1');
    expect(alertSpy.success).toHaveBeenCalled();
  });

  it('naoDeveExcluir_quandoCancelarConfirmacao', async () => {
    alertSpy.confirm.and.resolveTo({ isConfirmed: false });

    await component.remove('1');

    expect(storeSpy.delete{Entidade}).not.toHaveBeenCalled();
  });
});
```

---

## 3. Teste de Componente de Formulário

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { {Entidade}FormComponent } from './{entidade}-form.component';
import { {Entidade}Store } from '../services/{entidade}.store';
import { AlertService, NavigationService } from 'gems-sdk';

describe('{Entidade}FormComponent', () => {
  let fixture: ComponentFixture<{Entidade}FormComponent>;
  let component: {Entidade}FormComponent;
  let storeSpy: jasmine.SpyObj<{Entidade}Store>;
  let alertSpy: jasmine.SpyObj<AlertService>;

  const setupWithId = (id: string | null) => {
    return TestBed.configureTestingModule({
      imports: [{Entidade}FormComponent, ReactiveFormsModule],
      providers: [
        { provide: {Entidade}Store, useValue: storeSpy },
        { provide: AlertService,      useValue: alertSpy },
        { provide: NavigationService, useValue: jasmine.createSpyObj('NavigationService', ['navigate']) },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => id } } }
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  };

  beforeEach(async () => {
    storeSpy  = jasmine.createSpyObj('{Entidade}Store', ['getById', 'create', 'update']);
    alertSpy  = jasmine.createSpyObj('AlertService', ['success', 'warning', 'errorFromApi']);
  });

  describe('Modo criação (sem ID)', () => {
    beforeEach(async () => {
      await setupWithId(null);
      fixture   = TestBed.createComponent({Entidade}FormComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('deveInicializarModoInsercao', () => {
      expect(component.isEdit()).toBeFalse();
    });

    it('deveChamarCreate_quandoSalvar', () => {
      storeSpy.create.and.returnValue(of({ id: '1' } as any));
      component.{entidade}Form.patchValue({ campoObrigatorio: 'Valor' });
      component.save();
      expect(storeSpy.create).toHaveBeenCalled();
    });
  });

  describe('Modo edição (com ID)', () => {
    beforeEach(async () => {
      storeSpy.getById.and.returnValue(of({ id: '1', campoObrigatorio: 'Existente' } as any));
      await setupWithId('1');
      fixture   = TestBed.createComponent({Entidade}FormComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    it('deveInicializarModoEdicaoECarregarDados', () => {
      expect(component.isEdit()).toBeTrue();
      expect(storeSpy.getById).toHaveBeenCalledWith('1');
    });

    it('deveChamarUpdate_quandoSalvar', () => {
      storeSpy.update.and.returnValue(of({ id: '1' } as any));
      component.save();
      expect(storeSpy.update).toHaveBeenCalled();
    });
  });
});
```

---

## 4. Teste de Store

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { {Entidade}Store } from './{entidade}.store';

describe('{Entidade}Store', () => {
  let store: {Entidade}Store;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [{Entidade}Store]
    });
    store = TestBed.inject({Entidade}Store);
    http  = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('deveBuscarPorId', () => {
    const mockData = { id: '1', nome: 'Teste' };
    store.getById('1').subscribe(res => expect(res).toEqual(mockData as any));
    http.expectOne('api/{recurso}/1').flush(mockData);
  });

  it('devePesquisar', () => {
    const mockRes = { content: [], totalElements: 0 };
    store.search({}, { page: 0, size: 10, sort: [] }).subscribe(res => expect(res).toEqual(mockRes));
    http.expectOne(req => req.url.includes('/search') && req.method === 'POST').flush(mockRes);
  });
});
```

---

## 5. Padrões

### Nomenclatura obrigatória:
```
deveRetornarLista_quandoSearchComSucesso()
deveLancarErro_quandoServicoIndisponivel()
naoDevePersistir_quandoDadosInvalidos()
deveNavegar_quandoSalvarComSucesso()
```

### Regras:
| Regra | Detalhe |
| :--- | :--- |
| `NO_ERRORS_SCHEMA` em testes de componentes | Ignora gems-table, gems-form-card etc. — testa comportamento, não template |
| Mocks via `jasmine.createSpyObj` | Tipados com `jasmine.SpyObj<T>` |
| Nunca `HttpClientTestingModule` em testes de componentes | Só em testes de Store |
| `fixture.detectChanges()` após setup | Dispara `ngOnInit` e propagação de signals |
| `await fixture.whenStable()` | Para operações assíncronas (Promises + signals) |
| AAA obrigatório | Arrange → Act → Assert — sem misturar |

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `new {Entidade}Store()` no teste | `jasmine.createSpyObj<{Entidade}Store>('{Entidade}Store', ['search', 'create', ...])` |
| `HttpClientTestingModule` em testes de componentes | Apenas em testes de Store; componentes usam spy da Store |
| Esquecer `fixture.detectChanges()` após setup | Obrigatório para disparar `ngOnInit` e bindings de signals |
| `component.isLoading = true` (mutação direta) | `component.isLoading.set(true)` — signals são atualizados via `.set()` |
| Nome de teste: `it('testa busca', ...)` | `it('deveRetornarLista_quandoSearchComSucesso', ...)` — padrão de nomenclatura obrigatório |
