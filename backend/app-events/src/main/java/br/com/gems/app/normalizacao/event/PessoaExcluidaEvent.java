package br.com.gems.app.normalizacao.event;

import java.util.UUID;

/**
 * Publicado quando uma pessoa é excluída <b>logicamente</b>.
 *
 * <p>Quem escuta: o domínio {@code documento}, que arrasta os arquivos pessoais dela (A-E3).
 *
 * <p>Como a mesma {@code Pessoa} pode ser membro de mais de uma família, excluí-la retira os
 * documentos dela de <b>todas</b> — que é o comportamento correto: o dono do arquivo é a pessoa,
 * não o nó da árvore.
 */
public record PessoaExcluidaEvent( UUID pessoaId ) {
}
