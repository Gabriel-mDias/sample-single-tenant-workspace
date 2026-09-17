package br.com.gems.app.auditoria.interceptor;

import br.com.gems.app.model.enums.OperacaoAuditoria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Uma operação capturada pelo interceptador e ainda não gravada.
 *
 * <h2>Por que existe um objeto intermediário, em vez de persistir na hora</h2>
 * O interceptador é chamado <b>durante o flush</b>. Persistir uma entidade nova ali, no meio da
 * varredura que o Hibernate está fazendo do próprio contexto de persistência, é a receita para
 * {@code ConcurrentModificationException} e para ordem de INSERT imprevisível.
 *
 * <p>
 * Então o interceptador só <b>anota</b>, e quem grava é o {@link AuditoriaGravador}, no
 * {@code beforeCommit} — que ainda está dentro da transação, e é o que faz o requisito valer: se o dado
 * sofrer rollback, a trilha vai junto.
 *
 * <p>
 * O autor, a correlação e o instante são capturados <b>no momento da operação</b>, não no da
 * gravação. Os dois momentos estão na mesma thread e quase sempre no mesmo instante, mas capturar
 * cedo é o que mantém o registro fiel se algo entre um e outro mexer no contexto de segurança.
 *
 * @param campos
 *            vazia em {@code INSERCAO} e {@code EXCLUSAO} — o requisito pede a lista apenas na
 *            alteração.
 */
record RegistroPendente(
        String entidade,
        UUID registroAfetado,
        OperacaoAuditoria operacao,
        UUID autorId,
        String autorNome,
        String correlacaoId,
        LocalDateTime dataOperacao,
        List<CampoPendente> campos) {

    /** Um campo que mudou. Valores nulos quando {@code sigiloso} — é exigência da trilha. */
    record CampoPendente(String campo, String valorAnterior, String valorNovo, boolean sigiloso) {
    }
}
