package br.com.gems.app.autorizacao;

import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.support.AbstractIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Exercita a matriz aprovada de perfis em todos os 19 endpoints próprios. A expectativa de acesso
 * é declarada aqui, independentemente da anotação do controller e do mapa que cria as authorities.
 * IDs inexistentes e corpos inválidos evitam mutações: a passagem da camada de segurança pode
 * terminar em erro de negócio, mas nunca em 403 quando o perfil tem permissão.
 */
@AutoConfigureMockMvc
class AutorizacaoMatrizHttpIntegrationTest extends AbstractIntegrationTest {

    private static final EnumSet<PerfilAtuacao> TODOS = EnumSet.allOf( PerfilAtuacao.class );
    private static final EnumSet<PerfilAtuacao> ADMIN_OPERADOR = EnumSet.of( PerfilAtuacao.ADMINISTRADOR,
            PerfilAtuacao.OPERADOR );
    private static final EnumSet<PerfilAtuacao> ADMIN = EnumSet.of( PerfilAtuacao.ADMINISTRADOR );

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "{0}")
    @MethodSource("endpoints")
    void deveAplicarPerfilEmCadaEndpointProprio_quandoExecutaMatriz( Endpoint endpoint ) throws Exception {
        for ( PerfilAtuacao perfil : PerfilAtuacao.values() ) {
            var authorities = MapaPerfilAcoes.acoesDe( perfil ).stream()
                    .map( acao -> (GrantedAuthority) new SimpleGrantedAuthority( "ROLE_" + acao.name() ) )
                    .toList();
            var response = mockMvc.perform( request( endpoint ).with( jwt().authorities( authorities ) ) )
                    .andReturn().getResponse();
            if ( endpoint.permitidos().contains( perfil ) ) {
                assertThat( response.getStatus() ).as( "%s para %s deve atravessar a autorizacao",
                        endpoint, perfil ).isNotEqualTo( 403 );
            } else {
                assertThat( response.getStatus() ).as( "%s para %s deve falhar fechado",
                        endpoint, perfil ).isEqualTo( 403 );
            }
        }

        var semAcoes = mockMvc.perform( request( endpoint ).with( jwt().authorities( List.of() ) ) )
                .andReturn().getResponse();
        if ( endpoint.somenteAutenticado() ) {
            assertThat( semAcoes.getStatus() ).as( "%s autentico sem acoes", endpoint ).isNotEqualTo( 403 );
        } else {
            assertThat( semAcoes.getStatus() ).as( "%s sem acoes", endpoint ).isEqualTo( 403 );
        }

        var anonimo = mockMvc.perform( request( endpoint ) ).andReturn().getResponse();
        assertThat( anonimo.getStatus() ).as( "%s sem token", endpoint ).isEqualTo( 401 );
    }

    private static Stream<Endpoint> endpoints() {
        var id = UUID.randomUUID();
        return Stream.of(
                new Endpoint( "GET", "/api/parceiros/" + id, null, TODOS ),
                new Endpoint( "POST", "/api/parceiros/search", "{}", TODOS ),
                new Endpoint( "POST", "/api/parceiros", "{}", ADMIN_OPERADOR ),
                new Endpoint( "PUT", "/api/parceiros/" + id, "{}", ADMIN_OPERADOR ),
                new Endpoint( "DELETE", "/api/parceiros/" + id, null, ADMIN ),
                new Endpoint( "GET", "/api/solicitacoes/" + id, null, TODOS ),
                new Endpoint( "POST", "/api/solicitacoes/search", "{}", TODOS ),
                new Endpoint( "POST", "/api/solicitacoes", "{}", ADMIN_OPERADOR ),
                new Endpoint( "PUT", "/api/solicitacoes/" + id, "{}", ADMIN_OPERADOR ),
                new Endpoint( "DELETE", "/api/solicitacoes/" + id, null, ADMIN_OPERADOR ),
                new Endpoint( "POST", "/api/solicitacoes/" + id + "/abrir", null, ADMIN_OPERADOR ),
                new Endpoint( "POST", "/api/solicitacoes/" + id + "/iniciar", null, ADMIN_OPERADOR ),
                new Endpoint( "POST", "/api/solicitacoes/" + id + "/concluir", null, ADMIN_OPERADOR ),
                new Endpoint( "POST", "/api/solicitacoes/" + id + "/cancelar", "{}", ADMIN_OPERADOR ),
                new Endpoint( "GET", "/api/localidades/paises", null, TODOS, true ),
                new Endpoint( "GET", "/api/localidades/paises/1/estados", null, TODOS, true ),
                new Endpoint( "GET", "/api/enderecos/cep/invalido", null, TODOS, true ),
                new Endpoint( "GET", "/api/auditoria/Parceiro/" + id, null, ADMIN ),
                new Endpoint( "POST", "/api/auditoria/search", "{}", ADMIN ) );
    }

    private MockHttpServletRequestBuilder request( Endpoint endpoint ) {
        MockHttpServletRequestBuilder builder = switch ( endpoint.method() ) {
            case "GET" -> get( endpoint.path() );
            case "POST" -> post( endpoint.path() );
            case "PUT" -> put( endpoint.path() );
            case "DELETE" -> delete( endpoint.path() );
            default -> throw new IllegalArgumentException( endpoint.method() );
        };
        return endpoint.body() == null
                ? builder
                : builder.contentType( MediaType.APPLICATION_JSON )
                        .content( endpoint.body() );
    }

    private record Endpoint(String method, String path, String body, EnumSet<PerfilAtuacao> permitidos,
            boolean somenteAutenticado) {
        private Endpoint( String method, String path, String body, EnumSet<PerfilAtuacao> permitidos ) {
            this( method, path, body, permitidos, false );
        }

        @Override
        public String toString() {
            return method + " " + path;
        }
    }
}
