package br.com.gems.app.auditoria.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Nenhum serviço de domínio conhece a auditoria (SC-141).
 *
 * <h2>Por que isto precisa de teste, se é uma ausência</h2>
 * Porque é a ausência que <b>define</b> a rodada. O requisito exige que a trilha seja produzida pela
 * camada de persistência, e a alternativa recusada — uma chamada em cada {@code save} — não é
 * recusada por gosto: ela depende de lembrança, e o que depende de lembrança é esquecido.
 *
 * <p>
 * Sem esta varredura, a primeira rodada apressada acrescentaria um
 * {@code auditoriaService.registrar(...)} num serviço qualquer, o código passaria em revisão como
 * "explícito é melhor que implícito", e o requisito estaria violado sem que nenhum teste reclamasse.
 * A partir daí a cobertura volta a depender de disciplina — exatamente o que a rodada existe para
 * eliminar.
 *
 * <h2>Ela varre o código-fonte, e não o bytecode</h2>
 * Um import não utilizado some do bytecode e continua no fonte; e a mensagem de falha precisa
 * apontar um <b>arquivo e uma linha</b> para quem for corrigir. Varrer o fonte é o que dá as duas
 * coisas.
 */
class AuditoriaIsolamentoTest {

    /**
     * O pacote raiz do projeto, derivado do pacote <b>desta</b> classe.
     *
     * <p>
     * Cravar {@code "br.com.gems.app"} aqui faria o teste quebrar no dia em que o projeto fosse
     * renomeado — e quebrar por um motivo que não tem nada a ver com o que ele verifica. Derivar do
     * próprio pacote resolve: a classe está em {@code <raiz>.auditoria.interceptor}, então a raiz é
     * o que sobra ao tirar os dois últimos segmentos.
     */
    private static final String PACOTE_RAIZ = AuditoriaIsolamentoTest.class.getPackageName()
            .replaceFirst( "\\.auditoria\\.interceptor$", "" );

    private static final Path RAIZ = Path.of( "src", "main", "java" )
            .resolve( Path.of( "", PACOTE_RAIZ.split( "\\." ) ) );

    private static final String PACOTE_AUDITORIA = PACOTE_RAIZ + ".auditoria";

    @Test
    @DisplayName("SC-141: nenhum pacote de negócio referencia a auditoria")
    void deveComprovarNenhumServicoDeDominioReferenciaAuditoria_quandoExecutaOCenario() throws IOException {
        try ( Stream<Path> arquivos = Files.walk( RAIZ ) ) {
            var infratores = arquivos
                    .filter( caminho -> caminho.toString().endsWith( ".java" ) )
                    .filter( caminho -> !caminho.toString().replace( '\\', '/' ).contains( "/auditoria/" ) )
                    .filter( AuditoriaIsolamentoTest::referenciaAuditoria )
                    .map( RAIZ::relativize )
                    .map( Path::toString )
                    .toList();

            assertThat( infratores )
                    .as( "estes arquivos referenciam o pacote de auditoria. O requisito exige que a "
                            + "trilha seja produzida pela camada de persistência, sem NENHUMA chamada "
                            + "explícita — se a trilha precisa ser lembrada, ela será esquecida" )
                    .isEmpty();
        }
    }

    @Test
    @DisplayName("A varredura de fato enxerga o código — não passa por olhar para o vazio")
    void deveComprovarAVarreduraEnxergaOsArquivos_quandoExecutaOCenario() throws IOException {
        try ( Stream<Path> arquivos = Files.walk( RAIZ ) ) {
            var total = arquivos.filter( c -> c.toString().endsWith( ".java" ) ).count();

            // O piso é uma ordem de grandeza, não uma contagem: ele só precisa ser alto o bastante
            // para que um caminho quebrado (zero arquivo) reprove, e baixo o bastante para não
            // exigir manutenção a cada arquivo removido. Suba-o quando o projeto crescer.
            assertThat( total )
                    .as( "se o caminho da varredura quebrar, o teste acima passaria examinando zero "
                            + "arquivo — e diria que está tudo certo" )
                    .isGreaterThan( 20 );
        }
    }

    private static boolean referenciaAuditoria( Path caminho ) {
        try {
            return Files.readAllLines( caminho ).stream()
                    .anyMatch( linha -> linha.contains( PACOTE_AUDITORIA ) );
        } catch ( IOException e ) {
            throw new IllegalStateException( "não foi possível ler " + caminho, e );
        }
    }

    /** Documenta o que a varredura NÃO cobre, para quem for ampliá-la. */
    @Test
    @DisplayName("A própria auditoria pode se referenciar — ela está fora da varredura")
    void deveComprovarOPacoteDeAuditoriaEstaForaDaVarredura_quandoExecutaOCenario() {
        var dentroDaAuditoria = List.of(
                RAIZ.resolve( Path.of( "auditoria", "interceptor", "AuditoriaInterceptor.java" ) ).toString() );

        assertThat( dentroDaAuditoria )
                .allSatisfy( caminho -> assertThat( caminho.replace( '\\', '/' ) )
                        .contains( "/auditoria/" ) );
    }
}
