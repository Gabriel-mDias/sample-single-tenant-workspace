import { NO_ERRORS_SCHEMA, Signal, WritableSignal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { GemsAlertService, provideGemsHttp } from '@gabriel-mdias/angular-gems-sdk';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { SolicitacaoModel } from '../models/solicitacao.model';
import { SolicitacaoStore } from '../services/solicitacao.store';
import { SolicitacaoFormComponent } from './solicitacao-form.component';

describe('SolicitacaoFormComponent', () => {
  it('deveConsultarAcaoDeConcluir_quandoSolicitacaoEstaEmAndamento', () => {
    const isAuthorized = vi.fn(() => true);
    TestBed.configureTestingModule({
      imports: [SolicitacaoFormComponent],
      providers: [
        FormBuilder,
        provideHttpClient(),
        provideGemsHttp('http://localhost/api'),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: {}, paramMap: { get: () => null } } },
        },
        { provide: AuthService, useValue: { isAuthorized } },
        { provide: SolicitacaoStore, useValue: {} },
        { provide: GemsAlertService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });
    const component = TestBed.createComponent(SolicitacaoFormComponent)
      .componentInstance as unknown as {
      solicitacao: WritableSignal<SolicitacaoModel | null>;
      podeConcluir: Signal<boolean>;
    };
    component.solicitacao.set({ titulo: 'Solicitacao', situacao: 'EM_ANDAMENTO' });

    expect(component.podeConcluir()).toBe(true);
    expect(isAuthorized).toHaveBeenCalledWith({
      scope: 'global',
      actions: ['CONCLUIR_SOLICITACAO'],
    });
  });
});
