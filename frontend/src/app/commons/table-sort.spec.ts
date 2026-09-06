import { describe, expect, it } from 'vitest';
import { parseTableSort } from './table-sort';

describe('parseTableSort', () => {
  it('lê campo e direção do formato do Spring Data', () => {
    expect(parseTableSort(['nome,asc'])).toEqual({ field: 'nome', direction: 'asc' });
    expect(parseTableSort(['paisRepresentadoNome,desc'])).toEqual({
      field: 'paisRepresentadoNome',
      direction: 'desc',
    });
  });

  it('devolve o padrão quando a tabela emite sem ordenação', () => {
    const padrao = { field: 'nome', direction: 'asc' as const };

    expect(parseTableSort(undefined, padrao)).toEqual(padrao);
    expect(parseTableSort([], padrao)).toEqual(padrao);
    expect(parseTableSort([''], padrao)).toEqual(padrao);
  });

  it('trata qualquer direção que não seja "desc" como ascendente', () => {
    expect(parseTableSort(['nome'])).toEqual({ field: 'nome', direction: 'asc' });
    expect(parseTableSort(['nome,'])).toEqual({ field: 'nome', direction: 'asc' });
    expect(parseTableSort(['nome,qualquer'])).toEqual({ field: 'nome', direction: 'asc' });
  });

  it('sem padrão explícito, devolve campo indefinido e direção ascendente', () => {
    expect(parseTableSort(undefined)).toEqual({ field: undefined, direction: 'asc' });
  });
});
