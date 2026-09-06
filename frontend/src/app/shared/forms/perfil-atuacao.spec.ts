import { describe, expect, it } from 'vitest';
import { PERFIL_ATUACAO_OPTIONS, perfilAtuacaoLabel } from './perfil-atuacao';

/**
 * Trava a lista local contra o enum do backend.
 *
 * Os valores abaixo são transcritos de `PerfilAtuacao.java`. A duplicação é deliberada: é ela que
 * faz este teste quebrar quando um perfil entra num lugar só.
 */
describe('PERFIL_ATUACAO_OPTIONS', () => {
  const DO_BACKEND = ['ADMINISTRADOR', 'OPERADOR', 'CONSULTA'];

  it('tem exatamente os perfis do enum do backend, na mesma ordem', () => {
    expect(PERFIL_ATUACAO_OPTIONS.map((o) => o.value)).toEqual(DO_BACKEND);
  });

  it('nenhum rótulo é vazio', () => {
    PERFIL_ATUACAO_OPTIONS.forEach((o) => expect(o.label.trim()).not.toBe(''));
  });

  it('devolve o rótulo do perfil', () => {
    expect(perfilAtuacaoLabel('OPERADOR')).toBe('Operador');
  });

  it('perfil ausente ou desconhecido devolve string vazia, nunca undefined', () => {
    expect(perfilAtuacaoLabel(null)).toBe('');
    expect(perfilAtuacaoLabel(undefined)).toBe('');
    expect(perfilAtuacaoLabel('PERFIL_QUE_NAO_EXISTE')).toBe('');
  });
});
