import { describe, expect, it } from 'vitest';
import { PASSWORD_PATTERN, passwordValidator } from './password.validator';

const mockControl = (value: unknown) => ({ value }) as any;

describe('passwordValidator', () => {
  it('permite valores nulos ou vazios (deixando obrigatoriedade para Validators.required)', () => {
    expect(passwordValidator(mockControl(''))).toBeNull();
    expect(passwordValidator(mockControl(null))).toBeNull();
    expect(passwordValidator(mockControl(undefined))).toBeNull();
  });

  it('valida com sucesso senhas que atendem a todos os critérios', () => {
    expect(passwordValidator(mockControl('Senha@Forte123'))).toBeNull();
    expect(passwordValidator(mockControl('Admin#2026abc'))).toBeNull();
    expect(passwordValidator(mockControl('P@sswordLonga!'))).toBeNull();
    expect(passwordValidator(mockControl('aB1!aaaaaaaa'))).toBeNull();
  });

  it('rejeita senhas com menos de 12 caracteres', () => {
    expect(passwordValidator(mockControl('Aa@1234'))).toEqual({ passwordPattern: true });
    // 11 caracteres, forte em todo o resto: o limite é o que recusa, e é o que a PF-11 mudou.
    expect(passwordValidator(mockControl('Aa@1234567b'))).toEqual({ passwordPattern: true });
  });

  it('rejeita senhas sem letras maiúsculas', () => {
    expect(passwordValidator(mockControl('senha@forte123'))).toEqual({ passwordPattern: true });
  });

  it('rejeita senhas sem letras minúsculas', () => {
    expect(passwordValidator(mockControl('SENHA@FORTE123'))).toEqual({ passwordPattern: true });
  });

  it('rejeita senhas sem caracteres especiais', () => {
    expect(passwordValidator(mockControl('SenhaSemEspecial'))).toEqual({ passwordPattern: true });
  });

  it('rejeita senhas com espaços em branco', () => {
    expect(passwordValidator(mockControl('Senha @Forte123'))).toEqual({ passwordPattern: true });
  });

  it('funciona quando chamado como factory de ValidatorFn', () => {
    const fn = passwordValidator();
    expect(fn(mockControl('Senha@Forte123'))).toBeNull();
    expect(fn(mockControl('fraca'))).toEqual({ passwordPattern: true });
  });

  it('exporta regex PASSWORD_PATTERN correspondente ao backend', () => {
    expect(PASSWORD_PATTERN.test('Senha@Forte123')).toBe(true);
    expect(PASSWORD_PATTERN.test('invalida')).toBe(false);
  });
});
