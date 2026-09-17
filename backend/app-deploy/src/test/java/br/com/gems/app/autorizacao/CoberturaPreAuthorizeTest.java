package br.com.gems.app.autorizacao;

import br.com.gems.app.security.annotation.PublicEndpoint;
import br.com.gems.app.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Varredura de cobertura: todo endpoint de todo {@code @RestController} do sistema precisa declarar
 * {@code @PreAuthorize} — direto, ou por meio de {@code @PublicEndpoint} quando não exigir ação.
 *
 * <p>
 * <b>Por que este teste existe.</b> Sem ele, a única falha silenciosa possível nesta rodada é uma
 * anotação esquecida: o endpoint não quebra em lugar nenhum, apenas cai em
 * {@code anyRequest().authenticated()} do {@code SecurityConfig} e fica aberto a qualquer autenticado,
 * sem que nenhum teste de negócio o note (FR-015).
 *
 * <p>
 * <b>Não há lista de isenção por classe.</b> {@code LocalidadeController} e {@code CepController}
 * são dado de referência consumido por todo formulário do sistema, mas ainda assim carregam
 * {@code @PreAuthorize("isAuthenticated()")} em cada método — então nada precisa ser isento: exigir a
 * anotação em <b>todo</b> método mapeado é mais simples de manter do que uma lista que precisaria ser
 * atualizada a cada controller novo. A única saída deliberada é {@code @PublicEndpoint}, que já é o
 * mecanismo do projeto para "sem autenticação".
 *
 * <h2>Por que a varredura se restringe ao pacote do projeto</h2>
 * A primeira versão deste teste varria <b>todos</b> os {@code @RestController} do contexto e falhou
 * apontando seis métodos — três do {@code S3Controller} (do {@code gems-aws-web}) e três do
 * springdoc. O apontamento estava tecnicamente correto e mesmo assim era a pergunta errada: esses
 * controllers vêm de <b>jar de terceiro</b>, e não há como acrescentar {@code @PreAuthorize} a eles
 * sem forkar a biblioteca.
 *
 * <p>
 * Eles não estão desprotegidos — são tratados por <b>regra de caminho</b> no {@code SecurityConfig},
 * que é o único lugar de onde se pode alcançá-los:
 * <ul>
 * <li>{@code /api/aws/s3/**} tem {@code denyAll()}, e quem prova isso é o
 * {@code S3EndpointsSecurityIntegrationTest} — com 403 verificado nos três endpoints;</li>
 * <li>{@code /v3/api-docs/**} e {@code /swagger-ui/**} são deliberadamente públicos, declarados
 * entre os padrões fixos de {@code getPublicEndpoints()}.</li>
 * </ul>
 *
 * <p>
 * Restringir ao pacote {@code br.com.gems.app} mantém o teste respondendo à pergunta que ele
 * consegue responder — "algum endpoint <b>nosso</b> ficou sem regra?" —, em vez de exigir de terceiros
 * uma anotação que eles nunca terão e que só produziria uma lista de exceções crescendo a cada
 * dependência nova.
 */
class CoberturaPreAuthorizeTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    /**
     * Só os controllers escritos por este projeto. Controllers de terceiros são cobertos por regra de
     * caminho no {@code SecurityConfig} — ver o javadoc da classe.
     */
    private static final String PACOTE_DO_PROJETO = "br.com.gems.app";

    private static final List<Class<? extends Annotation>> ANOTACOES_DE_MAPEAMENTO = List.of(
            GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class,
            PatchMapping.class, RequestMapping.class );

    @Test
    @DisplayName("todo método mapeado de todo @RestController tem @PreAuthorize, salvo @PublicEndpoint")
    void deveComprovarTodoEndpointTemPreAuthorizeOuEhPublico_quandoExecutaOCenario() {
        var semAnotacao = new ArrayList<String>();

        for ( var bean : context.getBeansWithAnnotation( RestController.class ).values() ) {
            var classeAlvo = AopUtils.getTargetClass( bean );

            if ( !classeAlvo.getName().startsWith( PACOTE_DO_PROJETO ) ) {
                continue;
            }

            if ( classeAlvo.isAnnotationPresent( PublicEndpoint.class ) ) {
                continue;
            }

            for ( var metodo : classeAlvo.getMethods() ) {
                if ( !ehEndpointMapeado( metodo ) ) {
                    continue;
                }

                if ( metodo.isAnnotationPresent( PublicEndpoint.class ) ) {
                    continue;
                }

                if ( !metodo.isAnnotationPresent( PreAuthorize.class ) ) {
                    semAnotacao.add( classeAlvo.getSimpleName() + "#" + metodo.getName() );
                }
            }
        }

        assertThat( semAnotacao )
                .as( "métodos mapeados sem @PreAuthorize nem @PublicEndpoint: %s", semAnotacao )
                .isEmpty();
    }

    @Test
    @DisplayName("a varredura de fato encontra os 13 controllers do projeto — senão ela passaria vazia")
    void deveComprovarAVarreduraNaoPassaVazia_quandoExecutaOCenario() {
        var controllersDoProjeto = context.getBeansWithAnnotation( RestController.class ).values()
                .stream()
                .map( AopUtils::getTargetClass )
                .filter( classe -> classe.getName().startsWith( PACOTE_DO_PROJETO ) )
                .map( Class::getSimpleName )
                .sorted()
                .toList();

        // Sem esta asserção, um erro no filtro de pacote faria o teste acima passar por não olhar
        // nada — a pior forma de teste verde. Os cinco controllers abaixo são a lista fechada de
        // hoje; um controller novo quebra aqui, e quebrar aqui é a intenção: ele precisa entrar na
        // conta.
        //
        // Atualizar esta lista é tarefa do roteiro da rodada que acrescentou o controller, não
        // conserto de teste quebrado.
        assertThat( controllersDoProjeto ).containsExactly(
                "AuditoriaController", "CepController", "LocalidadeController",
                "ParceiroController", "SolicitacaoController" );
    }

    private boolean ehEndpointMapeado( Method metodo ) {
        return ANOTACOES_DE_MAPEAMENTO.stream().anyMatch( metodo::isAnnotationPresent );
    }

}
