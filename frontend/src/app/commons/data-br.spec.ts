import { describe, expect, it } from 'vitest';
import { dataBr, dataBrOu } from './data-br';

describe('dataBr', () => {
  it('converte a data ISO simples para o formato brasileiro', () => {
    expect(dataBr('2026-08-27')).toBe('27/08/2026');
  });

  it('aceita data-hora ISO, ignorando a parte de hora', () => {
    expect(dataBr('2026-08-27T10:30:00')).toBe('27/08/2026');
  });

  /**
   * ⚠️ A razão de a função cortar a string em vez de usar `new Date(valor)`.
   * `new Date('2026-01-01')` é interpretado como **UTC**, e em qualquer fuso negativo — o Brasil
   * inteiro — `getDate()` devolveria 31/12 do ano anterior. O defeito só apareceria na virada.
   */
  it('não desloca o dia por fuso horário', () => {
    expect(dataBr('2026-01-01')).toBe('01/01/2026');
    expect(dataBr('2026-01-01T00:00:00')).toBe('01/01/2026');
  });

  it('devolve null para ausência, em vez de "Invalid Date"', () => {
    expect(dataBr(null)).toBeNull();
    expect(dataBr(undefined)).toBeNull();
    expect(dataBr('')).toBeNull();
  });

  it('devolve null para entrada que não é data ISO', () => {
    expect(dataBr('ontem')).toBeNull();
  });
});

describe('dataBrOu', () => {
  it('usa o travessão como ausência por padrão — é o que a tabela do projeto exibe', () => {
    expect(dataBrOu(null)).toBe('—');
  });

  it('aceita um texto próprio de ausência', () => {
    expect(dataBrOu(null, 'Não vence')).toBe('Não vence');
  });

  it('formata normalmente quando há data', () => {
    expect(dataBrOu('2026-12-31')).toBe('31/12/2026');
  });
});
