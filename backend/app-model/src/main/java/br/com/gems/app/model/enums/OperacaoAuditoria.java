package br.com.gems.app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A natureza da operação registrada na trilha de auditoria.
 *
 * <p>Persistida na coluna {@code CD_OPERACAO} por nome ({@code @Enumerated(STRING)}), como todo
 * enum de domínio do projeto.
 *
 * @see br.com.gems.app.model.entity.auditoria.RegistroAuditoria
 */
@Getter
@RequiredArgsConstructor
public enum OperacaoAuditoria {

    /** O registro passou a existir. */
    INSERCAO( "Inserção" ),

    /** Um ou mais campos mudaram. */
    ALTERACAO( "Alteração" ),

    /** Exclusão lógica do registro de negócio. */
    EXCLUSAO( "Exclusão" );

    private final String descricao;

}
