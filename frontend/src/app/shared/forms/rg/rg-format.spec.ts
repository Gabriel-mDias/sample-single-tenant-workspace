import { formatRg, sanitizeRg } from './rg-format';

describe('rg-format', () => {
  it('formata o RG paulista com dígito verificador numérico', () => {
    expect(formatRg('123456789')).toBe('12.345.678-9');
  });

  it('preserva o dígito verificador alfabético do RG paulista', () => {
    expect(formatRg('12345678x')).toBe('12.345.678-X');
  });

  it('reconhece a sigla da UF e não inventa dígito verificador', () => {
    expect(formatRg('mg12345678')).toBe('MG-12.345.678');
  });

  it('não separa dígito verificador em RG curto', () => {
    expect(formatRg('1234567')).toBe('1.234.567');
  });

  it('acomoda emissores com mais de nove dígitos', () => {
    expect(formatRg('RS1234567890')).toBe('RS-1.234.567.890');
  });

  it('é idempotente sobre um valor já formatado', () => {
    for (const valor of ['12.345.678-9', 'MG-12.345.678', '12.345.678-X', '1.234.567']) {
      expect(formatRg(valor)).toBe(valor);
    }
  });

  it('não descarta caractere digitado dentro do limite', () => {
    const bruto = 'MG12345678';
    expect(sanitizeRg(formatRg(bruto))).toBe(bruto);
  });

  it('mantém o valor legível enquanto a sigla ainda está sendo digitada', () => {
    expect(formatRg('m')).toBe('M');
    expect(formatRg('mg')).toBe('MG');
    expect(formatRg('mg1')).toBe('MG-1');
  });

  it('nunca ultrapassa os 20 caracteres aceitos pelo cadastro', () => {
    expect(formatRg('12345678901234567890'.repeat(2)).length).toBeLessThanOrEqual(20);
    expect(formatRg('RS12345678901234567890').length).toBeLessThanOrEqual(20);
  });

  it('trata vazio, nulo e indefinido como string vazia', () => {
    expect(formatRg('')).toBe('');
    expect(formatRg(null)).toBe('');
    expect(formatRg(undefined)).toBe('');
    expect(sanitizeRg(null)).toBe('');
  });
});
