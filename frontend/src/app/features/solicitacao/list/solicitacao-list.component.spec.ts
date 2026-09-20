import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { GemsAlertService } from '@gabriel-mdias/angular-gems-sdk';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../../core/auth/auth.service';
import { SolicitacaoStore } from '../services/solicitacao.store';
import { SolicitacaoListComponent } from './solicitacao-list.component';

describe('SolicitacaoListComponent', () => {
  it('deveConsultarAcaoDeInserir_quandoCriarComponente', () => {
    const isAuthorized = vi.fn(() => false);
    TestBed.configureTestingModule({
      imports: [SolicitacaoListComponent],
      providers: [
        FormBuilder,
        provideRouter([]),
        { provide: AuthService, useValue: { isAuthorized } },
        { provide: SolicitacaoStore, useValue: { search: vi.fn() } },
        { provide: GemsAlertService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });

    TestBed.createComponent(SolicitacaoListComponent);

    expect(isAuthorized).toHaveBeenCalledWith({
      scope: 'global',
      actions: ['INSERIR_SOLICITACAO'],
    });
  });
});
