import { Routes } from '@angular/router';
import { roleGuard } from '../../core/guards/role.guard';
import { Acoes } from '../../core/auth/acoes';

/**
 * Rotas da feature de parceiros, registradas no `app.routes.ts` raiz por `loadChildren`.
 *
 * A ordem importa: `novo` e `:id/editar` precisam vir **antes** de `:id`, senão o parâmetro engole
 * as rotas literais.
 *
 * Os três modos são atendidos pelo mesmo orquestrador — o modo chega por `data.mode`.
 */
export const PARCEIRO_ROUTES: Routes = [
  {
    path: '',
    canActivate: [roleGuard],
    data: { roles: [Acoes.CONSULTAR_PARCEIRO] },
    loadComponent: () =>
      import('./list/parceiro-list.component').then((c) => c.ParceiroListComponent),
  },
  {
    path: 'novo',
    canActivate: [roleGuard],
    data: { roles: [Acoes.INSERIR_PARCEIRO], mode: 'create' },
    loadComponent: () =>
      import('./form/parceiro-form.component').then((c) => c.ParceiroFormComponent),
  },
  {
    path: ':id/editar',
    canActivate: [roleGuard],
    data: { roles: [Acoes.ALTERAR_PARCEIRO], mode: 'edit' },
    loadComponent: () =>
      import('./form/parceiro-form.component').then((c) => c.ParceiroFormComponent),
  },
  {
    path: ':id',
    canActivate: [roleGuard],
    data: { roles: [Acoes.CONSULTAR_PARCEIRO], mode: 'view' },
    loadComponent: () =>
      import('./form/parceiro-form.component').then((c) => c.ParceiroFormComponent),
  },
];
