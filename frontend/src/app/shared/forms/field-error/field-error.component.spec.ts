import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { GemsFieldErrorComponent } from '@gabriel-mdias/angular-gems-sdk';
import { beforeEach, describe, expect, it } from 'vitest';
import { FieldErrorComponent } from './field-error.component';

/**
 * O que este teste protege: a mensagem que diz ao operador **o que** está errado no campo.
 *
 * A borda vermelha do campo inválido é CSS puro e nunca quebrou. A mensagem, sim — e o modo como
 * ela quebrava era silencioso: nenhum erro no console, nenhum teste falhando, apenas um formulário
 * que recusa salvar sem explicar por quê. O último `it` mantém o defeito da SDK documentado por
 * execução, não por comentário: se um dia ele passar a falhar, é sinal de que a SDK corrigiu o
 * componente dela e o daqui pode ser aposentado.
 */
@Component({
  imports: [ReactiveFormsModule, FieldErrorComponent],
  template: `
    <input [formControl]="control()" />
    <app-field-error [control]="control()" [messages]="mensagens"></app-field-error>
  `,
})
class HospedeiroComponent {
  readonly control = signal(new FormControl('', Validators.required));
  readonly mensagens = { required: 'Informe o sobrenome.' };
}

/** Hospedeiro equivalente, montado sobre o componente da SDK — só para o teste de caracterização. */
@Component({
  imports: [ReactiveFormsModule, GemsFieldErrorComponent],
  template: `
    <input [formControl]="control" />
    <gems-field-error [control]="control" [messages]="mensagens"></gems-field-error>
  `,
})
class HospedeiroSdkComponent {
  readonly control = new FormControl('', Validators.required);
  readonly mensagens = { required: 'Informe o sobrenome.' };
}

describe('FieldErrorComponent', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  function montar() {
    const fixture = TestBed.createComponent(HospedeiroComponent);
    fixture.detectChanges();
    return fixture;
  }

  const texto = (fixture: { nativeElement: HTMLElement }) =>
    (fixture.nativeElement.querySelector('.field-error')?.textContent ?? '').trim();

  it('não exibe nada enquanto o campo está intocado', () => {
    const fixture = montar();
    expect(texto(fixture)).toBe('');
  });

  it('exibe a mensagem customizada depois de markAllAsTouched — o caso do botão salvar', () => {
    const fixture = montar();

    // É exatamente o que `saveAndNext()` faz quando o formulário está inválido.
    fixture.componentInstance.control().markAsTouched();
    fixture.detectChanges();

    expect(texto(fixture)).toBe('Informe o sobrenome.');
  });

  it('some quando o campo passa a ser válido', () => {
    const fixture = montar();
    fixture.componentInstance.control().markAsTouched();
    fixture.detectChanges();
    expect(texto(fixture)).toBe('Informe o sobrenome.');

    fixture.componentInstance.control().setValue('Rossi');
    fixture.detectChanges();

    expect(texto(fixture)).toBe('');
  });

  it('cai na mensagem padrão em pt-BR quando o validador não tem mensagem customizada', () => {
    const fixture = TestBed.createComponent(HospedeiroComponent);
    fixture.componentInstance.control.set(new FormControl('ab', Validators.minLength(5)));
    fixture.detectChanges();

    fixture.componentInstance.control().markAsTouched();
    fixture.detectChanges();

    expect(texto(fixture)).toBe('Valor muito curto.');
  });

  it('acompanha a troca do control informado no input', () => {
    const fixture = montar();
    const novo = new FormControl('', Validators.required);
    novo.markAsTouched();

    fixture.componentInstance.control.set(novo);
    fixture.detectChanges();

    expect(texto(fixture)).toBe('Informe o sobrenome.');
  });

  it('caracterização: o gems-field-error da SDK 1.1.0 continua sem exibir a mensagem', () => {
    const fixture = TestBed.createComponent(HospedeiroSdkComponent);
    fixture.detectChanges();

    fixture.componentInstance.control.markAsTouched();
    fixture.detectChanges();

    const exibido = (
      fixture.nativeElement.querySelector('.gems-field-error')?.textContent ?? ''
    ).trim();

    expect(exibido).toBe('');
  });
});
