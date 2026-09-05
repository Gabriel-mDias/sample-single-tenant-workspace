package br.com.gems.app.autorizacao;

import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.security.AcaoSistema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava a matriz de permissões contra o contrato aprovado.
 *
 * <p>As contagens abaixo são <b>transcritas à mão</b> do contrato da rodada, e é essa duplicação
 * deliberada que dá valor ao teste: alterar o mapa sem alterar o contrato quebra aqui. Um teste que
 * derivasse os números do próprio mapa passaria sempre, e não provaria nada.
 *
 * <p>O administrador não aparece nas contagens: ele é {@code EnumSet.allOf} por construção, e
 * cravar um número para ele criaria a segunda lista que o desenho existe para evitar.
 */
class MapaPerfilAcoesTest {

    /** Totais do contrato, por perfil. */
    private static final Map<PerfilAtuacao, Integer> TOTAIS_DO_CONTRATO = Map.of(
            PerfilAtuacao.OPERADOR, 14,
            PerfilAtuacao.CONSULTA, 2
    );

    @Test
    @DisplayName( "as contagens por perfil batem com o contrato aprovado" )
    void contagensBatemComOContrato() {
        TOTAIS_DO_CONTRATO.forEach( ( perfil, total ) ->
                assertThat( MapaPerfilAcoes.acoesDe( perfil ) )
                        .as( "ações de %s", perfil )
                        .hasSize( total ) );
    }

    @Test
    @DisplayName( "o administrador recebe todas as ações do enum, por construção" )
    void administradorRecebeTudo() {
        assertThat( MapaPerfilAcoes.acoesDe( PerfilAtuacao.ADMINISTRADOR ) )
                .as( "acrescentar uma ação ao enum deve alcançar o administrador sem editar o mapa" )
                .hasSize( AcaoSistema.values().length )
                .containsExactlyInAnyOrder( AcaoSistema.values() );
    }

    @ParameterizedTest
    @EnumSource( PerfilAtuacao.class )
    @DisplayName( "todo perfil declarado tem entrada no mapa" )
    void todoPerfilTemEntrada( PerfilAtuacao perfil ) {
        assertThat( MapaPerfilAcoes.mapa() )
                .as( "perfil sem entrada devolveria conjunto vazio em silêncio, e ninguém veria" )
                .containsKey( perfil );
    }

    @Test
    @DisplayName( "o perfil de consulta não possui nenhuma ação de escrita" )
    void consultaSoLe() {
        assertThat( MapaPerfilAcoes.acoesDe( PerfilAtuacao.CONSULTA ) )
                .allSatisfy( acao -> assertThat( acao.name() ).startsWith( "CONSULTAR_" ) );
    }

    @Test
    @DisplayName( "a comissão é privativa do administrador" )
    void comissaoNaoVazaParaOsDemais() {
        assertThat( MapaPerfilAcoes.acoesDe( PerfilAtuacao.OPERADOR ) )
                .doesNotContain( AcaoSistema.VISUALIZAR_COMISSAO );
        assertThat( MapaPerfilAcoes.acoesDe( PerfilAtuacao.CONSULTA ) )
                .doesNotContain( AcaoSistema.VISUALIZAR_COMISSAO );
    }

    @Test
    @DisplayName( "excluir parceiro é privativo do administrador" )
    void exclusaoDeCadastroDeReferenciaEhRestrita() {
        assertThat( MapaPerfilAcoes.acoesDe( PerfilAtuacao.OPERADOR ) )
                .doesNotContain( AcaoSistema.EXCLUIR_PARCEIRO );
        assertThat( MapaPerfilAcoes.acoesDe( PerfilAtuacao.CONSULTA ) )
                .doesNotContain( AcaoSistema.EXCLUIR_PARCEIRO );
    }

    @Test
    @DisplayName( "o operador conduz o fluxo da solicitação de ponta a ponta" )
    void operadorConduzOFluxo() {
        assertThat( MapaPerfilAcoes.acoesDe( PerfilAtuacao.OPERADOR ) )
                .contains( AcaoSistema.ABRIR_SOLICITACAO,
                        AcaoSistema.INICIAR_SOLICITACAO,
                        AcaoSistema.CONCLUIR_SOLICITACAO,
                        AcaoSistema.CANCELAR_SOLICITACAO );
    }

    @Test
    @DisplayName( "cada conjunto do mapa é imutável" )
    void conjuntoEhImutavel() {
        var acoes = MapaPerfilAcoes.acoesDe( PerfilAtuacao.CONSULTA );
        Assertions.assertThrows( UnsupportedOperationException.class,
                () -> acoes.add( AcaoSistema.EXCLUIR_PARCEIRO ) );
    }
}
