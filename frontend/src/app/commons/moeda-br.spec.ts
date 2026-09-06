import { describe, expect, it } from 'vitest';
import { formatarMoedaBr, parseMoedaBr } from './moeda-br';

/**
 * O que este teste protege: um valor monetário exibido ao usuário. Um erro de
 * separador aqui não quebra nada visivelmente — grava um número plausível e errado, e o operador
 * só descobre no orçamento.
 */
describe('parseMoedaBr', () => {
  it('lê o formato brasileiro, com ponto de milhar e vírgula decimal', () => {
    expect(parseMoedaBr('1.234,56')).toBe(1234.56);
    expect(parseMoedaBr('12.345.678,90')).toBe(12345678.9);
  });

  it('lê também o formato com ponto decimal, que é como o teclado numérico produz', () => {
    expect(parseMoedaBr('1234.56')).toBe(1234.56);
    expect(parseMoedaBr('1.5')).toBe(1.5);
  });

  it('lê o formato en-US, com vírgula de milhar e ponto decimal', () => {
    expect(parseMoedaBr('1,234.56')).toBe(1234.56);
  });

  it('trata separador repetido como milhar, nos dois sentidos', () => {
    expect(parseMoedaBr('12.345.678')).toBe(12345678);
    expect(parseMoedaBr('1,234,567')).toBe(1234567);
  });

  it('lê inteiros nos dois formatos', () => {
    expect(parseMoedaBr('2500')).toBe(2500);
    expect(parseMoedaBr('2.500')).toBe(2500);
  });

  it('ignora espaços em volta e no meio', () => {
    expect(parseMoedaBr('  1 234,56 ')).toBe(1234.56);
  });

  it('devolve null para vazio — valor é opcional, e ausente não é zero', () => {
    expect(parseMoedaBr('')).toBeNull();
    expect(parseMoedaBr('   ')).toBeNull();
    expect(parseMoedaBr(null)).toBeNull();
    expect(parseMoedaBr(undefined)).toBeNull();
  });

  it('devolve null para texto que não vira número, em vez de NaN', () => {
    expect(parseMoedaBr('grátis')).toBeNull();
    expect(parseMoedaBr('R$')).toBeNull();
  });
});

describe('formatarMoedaBr', () => {
  it('formata com duas casas e separador brasileiro', () => {
    expect(formatarMoedaBr(1234.5)).toBe('1.234,50');
    expect(formatarMoedaBr(2500)).toBe('2.500,00');
  });

  it('devolve string vazia quando não há valor', () => {
    expect(formatarMoedaBr(null)).toBe('');
    expect(formatarMoedaBr(undefined)).toBe('');
  });

  it('é a volta do parse: o que sai da tela e volta para ela não muda', () => {
    expect(parseMoedaBr(formatarMoedaBr(1234.56))).toBe(1234.56);
  });
});
