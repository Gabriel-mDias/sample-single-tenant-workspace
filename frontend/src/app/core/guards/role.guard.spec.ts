import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Acoes } from '../auth/acoes';
import { AuthService } from '../auth/auth.service';
import { roleGuard } from './role.guard';

function configure(auth: Partial<AuthService>): void {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
  });
}

function runGuard(route: Partial<ActivatedRouteSnapshot>) {
  return TestBed.runInInjectionContext(() =>
    roleGuard(route as ActivatedRouteSnapshot, { url: '/parceiros' } as RouterStateSnapshot),
  );
}

describe('roleGuard', () => {
  beforeEach(() => {
    vi.stubGlobal('location', { origin: 'http://localhost:4200' });
  });

  it('não logado ⇒ dispara login e nega', () => {
    const login = vi.fn();
    configure({ isLoggedIn: () => false, login });

    const result = runGuard({ data: { roles: [Acoes.CONSULTAR_PARCEIRO] } });

    expect(result).toBe(false);
    expect(login).toHaveBeenCalledWith('http://localhost:4200/parceiros');
  });

  it('logado sem data.roles ⇒ negado (rota mal configurada falha fechada)', () => {
    configure({ isLoggedIn: () => true, hasAnyRole: vi.fn(() => true) });
    const router = TestBed.inject(Router);

    const result = runGuard({ data: {} });

    expect(result).toEqual(router.createUrlTree(['/acesso-negado']));
  });

  it('logado com a ação exigida ⇒ libera', () => {
    configure({
      isLoggedIn: () => true,
      hasAnyRole: (roles) => roles.includes(Acoes.CONSULTAR_PARCEIRO),
    });

    const result = runGuard({ data: { roles: [Acoes.CONSULTAR_PARCEIRO] } });

    expect(result).toBe(true);
  });

  it('logado sem a ação exigida ⇒ redireciona para /acesso-negado', () => {
    configure({ isLoggedIn: () => true, hasAnyRole: () => false });
    const router = TestBed.inject(Router);

    const result = runGuard({ data: { roles: [Acoes.EXCLUIR_PARCEIRO] } });

    expect(result).toEqual(router.createUrlTree(['/acesso-negado']));
  });
});
