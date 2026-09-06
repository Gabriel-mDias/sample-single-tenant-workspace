import { FormBuilder } from '@angular/forms';
import { describe, expect, it } from 'vitest';
import { SEXO_OPTIONS, createDadosPessoaisForm, sexoLabel } from './dados-pessoais-form';

describe('createDadosPessoaisForm', () => {
  it('mantém a ordem canônica dos dados pessoais', () => {
    const form = createDadosPessoaisForm(new FormBuilder());

    expect(Object.keys(form.controls)).toEqual([
      'primeiroNome',
      'sobrenome',
      'cpf',
      'rg',
      'sexo',
      'dataNascimento',
    ]);
  });

  it('exige os campos obrigatórios e CPF com onze dígitos', () => {
    const form = createDadosPessoaisForm(new FormBuilder());
    const cpf = form.get('cpf')!;

    expect(form.invalid).toBe(true);

    cpf.setValue('1234567890');
    expect(cpf.hasError('minlength')).toBe(true);

    cpf.setValue('12345678901');
    expect(cpf.valid).toBe(true);

    cpf.setValue('123456789012');
    expect(cpf.hasError('maxlength')).toBe(true);
  });

  it('exige o sexo e o inicializa em null para o placeholder aparecer', () => {
    const form = createDadosPessoaisForm(new FormBuilder());
    const sexo = form.get('sexo')!;

    // `null`, não `''`: o `gems-select` só marca a opção de placeholder quando o valor é nulo.
    expect(sexo.value).toBeNull();
    expect(sexo.hasError('required')).toBe(true);

    sexo.setValue('NAO_INFORMADO');
    expect(sexo.valid).toBe(true);
  });

  it('rotula as três opções de sexo e devolve travessão quando não há valor', () => {
    expect(SEXO_OPTIONS.map((opcao) => opcao.value)).toEqual([
      'MASCULINO',
      'FEMININO',
      'NAO_INFORMADO',
    ]);
    expect(sexoLabel('MASCULINO')).toBe('Masculino');
    expect(sexoLabel('NAO_INFORMADO')).toBe('Não informado');
    expect(sexoLabel(null)).toBe('—');
    expect(sexoLabel(undefined)).toBe('—');
  });
});
