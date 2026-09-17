package br.com.gems.app.auditoria.interceptor;

import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.entity.auditoria.CampoAlterado;
import br.com.gems.app.model.entity.auditoria.RegistroAuditoria;
import br.com.gems.app.model.entity.solicitacao.EtapaSolicitacao;
import br.com.gems.app.model.entity.solicitacao.Solicitacao;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A fronteira do que é auditado, exercitada diretamente.
 *
 * <h2>Por que este teste existe, sendo que já há teste de integração do mecanismo</h2>
 * Porque o de integração <b>não discrimina</b>. Uma prova por mutação desligou
 * a guarda de recursão e o teste "não existe trilha da trilha" continuou <b>verde</b>: com a guarda
 * fora, os registros da própria trilha até chegam a ser acumulados, mas o gravador consome uma cópia
 * do acúmulo numa passada só, e o que entra durante a escrita nunca é gravado. O efeito observável é
 * o mesmo com e sem a proteção.
 *
 * <p>
 * Uma rede que passa nos dois casos não prova nada. Este teste ataca a regra em si, e falha no
 * instante em que ela for afrouxada.
 *
 * <p>
 * Não precisa de Spring nem de banco: é uma decisão sobre tipos, e roda em milissegundos.
 */
class AuditoriaInterceptorTest {

    private final AuditoriaInterceptor interceptor = new AuditoriaInterceptor( null, null );

    @Nested
    @DisplayName("É auditada toda entidade de negócio")
    class EntidadesDeNegocio {

        @Test
        @DisplayName("as entidades que estendem EntidadeAuditavel são auditadas")
        void deveComprovarEntidadesAuditaveisSaoAuditadas_quandoExecutaOCenario() {
            assertThat( interceptor.deveAuditar( new Pessoa() ) ).isTrue();
            assertThat( interceptor.deveAuditar( new EtapaSolicitacao() ) ).isTrue();
            assertThat( interceptor.deveAuditar( new Parceiro() ) ).isTrue();
            assertThat( interceptor.deveAuditar( new Solicitacao() ) ).isTrue();
        }
    }

    @Nested
    @DisplayName("Não é auditado o que não é entidade de negócio")
    class ForaDaFronteira {

        @Test
        @DisplayName("a própria trilha nunca é auditada — seria recursão sem fim")
        void deveComprovarTrilhaNaoAuditaASiMesma_quandoExecutaOCenario() {
            assertThat( interceptor.deveAuditar( new RegistroAuditoria() ) ).isFalse();
            assertThat( interceptor.deveAuditar( new CampoAlterado() ) ).isFalse();
        }

        @Test
        @DisplayName("tabelas puramente referenciais ficam de fora (Princípio II já as isenta)")
        void deveComprovarReferenciaisNaoSaoAuditadas_quandoExecutaOCenario() {
            assertThat( interceptor.deveAuditar( new Pais() ) ).isFalse();
            assertThat( interceptor.deveAuditar( new Estado() ) ).isFalse();
        }

        @Test
        @DisplayName("objeto que não é entidade nenhuma não quebra o interceptador")
        void deveComprovarObjetoQualquerNaoEhAuditado_quandoExecutaOCenario() {
            assertThat( interceptor.deveAuditar( "uma string" ) ).isFalse();
            assertThat( interceptor.deveAuditar( new Object() ) ).isFalse();
        }
    }
}
