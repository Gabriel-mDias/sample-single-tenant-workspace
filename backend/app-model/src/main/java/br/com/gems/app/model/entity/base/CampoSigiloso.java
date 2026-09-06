package br.com.gems.app.model.entity.base;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um campo cujo <b>valor</b> nunca pode ser gravado na trilha de auditoria.
 *
 * <h2>O que ela faz, e o que deliberadamente não faz</h2>
 * A trilha registra que o campo <b>mudou</b> — o nome dele, e o instante — mas grava
 * {@code DS_VALOR_ANTERIOR} e {@code DS_VALOR_NOVO} nulos, com {@code FL_SIGILOSO} verdadeiro. A tela
 * mostra "valor não registrado".
 *
 * <p>Ela <b>não</b> controla quem enxerga o campo na API: isso continua sendo trabalho do serviço,
 * que consulta {@code UsuarioAutenticadoService.possuiAcao()} e omite o valor na leitura e na
 * gravação. As duas coisas são complementares, e é por isso que esta anotação existe.
 *
 * <h2>Por que anotação, e não uma lista no interceptador</h2>
 * Uma lista central de "campos sigilosos" é exatamente o tipo de rede que envelhece: ela vive longe
 * do campo que protege, e quem acrescenta um campo sigiloso numa rodada futura não tem motivo nenhum
 * para abri-la. A anotação viaja junto do campo — some com ele, é renomeada com ele, e aparece na
 * revisão do mesmo diff.
 *
 * <p>É a mesma lição que o projeto pagou com o {@code routes-coverage.spec.ts}, que enumerava as
 * tabelas de rota à mão e ignorou uma feature inteira por ela nunca ter entrado na lista.
 *
 * <h2>Por que o valor não é gravado nem cifrado</h2>
 * Cifrar exigiria uma chave, e chave existe para vazar. O sigilo da comissão fecha a porta da
 * frente — a Secretária abre a tela do vínculo e não vê o valor; gravá-lo em claro, ou recuperável,
 * na trilha abriria a porta lateral para o mesmo dado.
 *
 * @see br.com.gems.app.model.entity.auditoria.CampoAlterado
 */
@Documented
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface CampoSigiloso {
}
