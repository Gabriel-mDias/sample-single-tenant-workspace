import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';
import { AbstractControl } from '@angular/forms';

/** Mensagens padrão em pt-BR para os validadores nativos do Angular. */
const MENSAGENS_PADRAO: Record<string, string> = {
  required: 'Este campo é obrigatório.',
  email: 'Informe um e-mail válido.',
  minlength: 'Valor muito curto.',
  maxlength: 'Valor muito longo.',
  min: 'Valor abaixo do mínimo permitido.',
  max: 'Valor acima do máximo permitido.',
  pattern: 'Formato inválido.',
};

/**
 * Mensagem de erro de campo — substituto do `gems-field-error` da SDK npm 1.1.0.
 *
 * <h2>Por que o projeto tem o seu</h2>
 * O componente da SDK **nunca exibe mensagem nenhuma**. O `errorMessage` dele é um `computed()` que
 * lê `control.errors` e `control.touched`, e **nenhum dos dois é sinal**: as únicas dependências
 * reativas são os próprios `input()`, cuja referência não muda. O `computed` calcula `''` uma vez,
 * na primeira renderização — quando o campo ainda está intocado —, e não reavalia nunca mais.
 *
 * <p>O efeito prático atingia **todos** os formulários do projeto: ao clicar em salvar com o
 * formulário incompleto, o `markAllAsTouched()` acendia a borda vermelha (isso é CSS de
 * `ng-invalid`/`ng-touched`, não depende de sinal) e o texto que diz **o que** está errado não
 * aparecia. O operador via um campo vermelho sem explicação.
 *
 * <h2>O que muda aqui</h2>
 * A reatividade vem de `AbstractControl.events`, o fluxo que o próprio Angular emite a cada
 * mudança de valor, status ou estado de toque — incluindo o `TouchedChangeEvent` que o
 * `markAllAsTouched()` dispara, que é exatamente o momento em que a mensagem precisa aparecer.
 * Cada evento incrementa `revisao`, e é dele que o `computed` depende.
 *
 * <p>A regra de exibição é a mesma da SDK, de propósito: só mostra com o control `touched` ou
 * `dirty`, e só a **primeira** chave de erro — para não empilhar três linhas vermelhas sob um campo.
 * As mensagens customizadas continuam sendo mescladas sobre as padrão pelo input `messages`.
 *
 * <p>Quando a SDK corrigir o componente dela, este some e a substituição é textual. Até lá, é este
 * que deve ser usado: `<app-field-error [control]="..." [messages]="...">`.
 */
@Component({
  selector: 'app-field-error',
  templateUrl: './field-error.component.html',
  styleUrl: './field-error.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FieldErrorComponent {
  readonly control = input<AbstractControl | null>(null);
  readonly messages = input<Record<string, string>>({});

  /**
   * Contador de revisões do control. Não carrega informação — existe só para dar ao `computed`
   * abaixo uma dependência que de fato muda quando o estado do campo muda.
   */
  private readonly revisao = signal(0);

  constructor() {
    effect((onCleanup) => {
      const ctrl = this.control();

      // Trocar de control também é uma revisão: o novo pode já chegar tocado e inválido.
      this.revisao.update((v) => v + 1);

      if (!ctrl) {
        return;
      }

      const inscricao = ctrl.events.subscribe(() => this.revisao.update((v) => v + 1));
      onCleanup(() => inscricao.unsubscribe());
    });
  }

  protected readonly mensagem = computed<string>(() => {
    this.revisao();

    const ctrl = this.control();

    if (!ctrl || !ctrl.errors || !(ctrl.touched || ctrl.dirty)) {
      return '';
    }

    const primeiraChave = Object.keys(ctrl.errors)[0];
    const mescladas = { ...MENSAGENS_PADRAO, ...this.messages() };

    return mescladas[primeiraChave] ?? `Erro: ${primeiraChave}`;
  });
}
