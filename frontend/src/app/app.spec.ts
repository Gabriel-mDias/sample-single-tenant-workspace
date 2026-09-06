import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';
import { afterEach, describe, expect, it } from 'vitest';

import { App } from './app';
import { Acoes } from './core/auth/acoes';

/**
 * Testa a casca real (`App`) via TestBed, exercitando os `computed` do próprio componente
 * (`showShell`, `menuConfig`) — não cópias da lógica. O `KeycloakService` é stubado para simular
 * diferentes perfis de token sem subir o Keycloak.
 */
function keycloakStub(overrides: {
  loggedIn?: boolean;
  roles?: string[];
  tokenParsed?: Record<string, unknown>;
}): Partial<KeycloakService> {
  return {
    isLoggedIn: () => overrides.loggedIn ?? false,
    getUserRoles: () => overrides.roles ?? [],
    getKeycloakInstance: () =>
      ({ tokenParsed: overrides.tokenParsed ?? {} }) as ReturnType<
        KeycloakService['getKeycloakInstance']
      >,
  };
}

function createApp(stub: Partial<KeycloakService>): App {
  TestBed.configureTestingModule({
    imports: [App],
    providers: [provideRouter([]), { provide: KeycloakService, useValue: stub }],
  });
  return TestBed.createComponent(App).componentInstance;
}

describe('App (casca)', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('cria o componente', () => {
    const app = createApp(keycloakStub({ loggedIn: false }));
    expect(app).toBeTruthy();
  });

  it('oculta a casca quando não autenticado', () => {
    const app = createApp(keycloakStub({ loggedIn: false }));
    expect(app.showShell()).toBe(false);
  });

  it('mostra a casca quando autenticado (rota inicial)', () => {
    const app = createApp(keycloakStub({ loggedIn: true }));
    expect(app.showShell()).toBe(true);
  });

  it('com as ações de consulta → menu com os recursos correspondentes', () => {
    const app = createApp(
      keycloakStub({
        loggedIn: true,
        roles: [Acoes.CONSULTAR_PARCEIRO, Acoes.CONSULTAR_SOLICITACAO],
      }),
    );
    expect(app.menuConfig().items.map((i) => i.route)).toEqual(['/parceiros', '/solicitacoes']);
  });

  it('sem ação de consulta nenhuma → menu vazio', () => {
    const app = createApp(keycloakStub({ loggedIn: true, roles: [] }));
    expect(app.menuConfig().items.map((i) => i.route)).toEqual([]);
  });
});
