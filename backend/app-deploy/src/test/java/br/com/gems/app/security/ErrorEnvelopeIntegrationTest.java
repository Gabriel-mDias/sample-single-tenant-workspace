package br.com.gems.app.security;

import br.com.gems.app.commons.exception.ProvedorExternoException;
import br.com.gems.app.support.AbstractIntegrationTest;
import br.com.gems.exception.exception.BusinessException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(ErrorEnvelopeIntegrationTest.ErrorEndpointConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class ErrorEnvelopeIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE_PATH = "/test/error-envelope";

    @jakarta.annotation.Resource
    private MockMvc mockMvc;

    @Test
    void deveResponderComEnvelopeSdk_quandoProvedorExternoFalha() throws Exception {
        var resultado = mockMvc.perform( get( BASE_PATH + "/provedor" ).with( jwt() ) );

        assertEnvelope( resultado, 502, "SERVICO_INDISPONIVEL", BASE_PATH + "/provedor", "GET" )
                .andExpect( jsonPath( "$.message" ).value( "Falha funcional preservada." ) );
    }

    @Test
    void deveResponderComEnvelopeSdk_quandoAcessoENegado() throws Exception {
        var resultado = mockMvc.perform( get( BASE_PATH + "/restrito" ).with( jwt().authorities( List.of() ) ) );

        assertEnvelope( resultado, 403, "ACESSO_NEGADO", BASE_PATH + "/restrito", "GET" )
                .andExpect( jsonPath( "$.message" ).value( "Você não possui acesso para este serviço!" ) );
    }

    @Test
    void deveResponderComEnvelopeSdk_quandoRegraDeNegocioFalha() throws Exception {
        var resultado = mockMvc.perform( get( BASE_PATH + "/negocio" ).with( jwt() ) );

        assertEnvelope( resultado, 400, "FALHA", BASE_PATH + "/negocio", "GET" )
                .andExpect( jsonPath( "$.message" ).value( "Regra de teste recusou a operação." ) );
    }

    @Test
    void deveOcultarCausaInterna_quandoErroInesperado() throws Exception {
        var resultado = mockMvc.perform( get( BASE_PATH + "/inesperado" ).with( jwt() ) );

        assertEnvelope( resultado, 500, "ERRO_NAO_ESPERADO", BASE_PATH + "/inesperado", "GET" )
                .andExpect( content().string( not( containsString( "segredo-interno" ) ) ) );
    }

    @Test
    void deveOmitirDadosDaRequisicaoECausa_quandoHandlersRegistramErros( CapturedOutput output ) throws Exception {
        var segredo = "segredo-sentinela-p06-987654321";

        mockMvc.perform( get( BASE_PATH + "/inesperado/" + segredo ).with( jwt() ) )
                .andExpect( status().isInternalServerError() );
        mockMvc.perform( get( BASE_PATH + "/provedor/" + segredo ).with( jwt() ) )
                .andExpect( status().isBadGateway() );

        assertThat( output.getOut() + output.getErr() ).doesNotContain( segredo );
    }

    @Test
    void deveNegarRequisicaoSemCredencial_quandoNaoAutenticado() throws Exception {
        mockMvc.perform( get( BASE_PATH + "/provedor" ) )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void deveResponderComEnvelopeSdkSemEcoarEntrada_quandoRecursoNaoExiste() throws Exception {
        var entrada = "segredo-nao-existe";
        var caminho = BASE_PATH + "/" + entrada;
        var resultado = mockMvc.perform( get( caminho ).with( jwt() ) );

        assertEnvelope( resultado, 404, "FALHA", caminho, "GET" )
                .andExpect( jsonPath( "$.codigo" ).value( "RECURSO_NAO_ENCONTRADO" ) )
                .andExpect( jsonPath( "$.message" ).value( "Recurso não encontrado." ) )
                .andExpect( jsonPath( "$.message" ).value( not( containsString( entrada ) ) ) );
    }

    @Test
    void deveResponderComEnvelopeSdkSemEcoarEntrada_quandoTipoDaRotaEInvalido() throws Exception {
        var entrada = "uuid-controlado-invalido";
        var caminho = BASE_PATH + "/uuid/" + entrada;
        var resultado = mockMvc.perform( get( caminho ).with( jwt() ) );

        assertEnvelope( resultado, 400, "VALIDACAO", caminho, "GET" )
                .andExpect( jsonPath( "$.codigo" ).value( "PARAMETRO_INVALIDO" ) )
                .andExpect( jsonPath( "$.message" ).value( "O parâmetro 'id' foi informado em um formato inválido." ) )
                .andExpect( jsonPath( "$.message" ).value( not( containsString( entrada ) ) ) );
    }

    private ResultActions assertEnvelope( ResultActions resultado, int statusEsperado, String tipoEsperado,
            String caminhoEsperado, String metodoEsperado ) throws Exception {
        return resultado
                .andExpect( status().is( statusEsperado ) )
                .andExpect( content().contentTypeCompatibleWith( MediaType.APPLICATION_JSON ) )
                .andExpect( jsonPath( "$.occurrenceTime" ).exists() )
                .andExpect( jsonPath( "$.errorType" ).value( tipoEsperado ) )
                .andExpect( jsonPath( "$.message" ).isNotEmpty() )
                .andExpect( jsonPath( "$.path" ).value( caminhoEsperado ) )
                .andExpect( jsonPath( "$.method" ).value( metodoEsperado ) );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ErrorEndpointConfiguration {

        @Bean
        ErrorEndpointController errorEndpointController() {
            return new ErrorEndpointController();
        }
    }

    @RestController
    @RequestMapping(BASE_PATH)
    static class ErrorEndpointController {

        @GetMapping("/provedor")
        @PreAuthorize("isAuthenticated()")
        void provedor() {
            throw new ProvedorExternoException( "Falha funcional preservada.", new IOException( "causa" ) );
        }

        @GetMapping("/provedor/{valor}")
        @PreAuthorize("isAuthenticated()")
        void provedorComEntrada( @PathVariable String valor ) {
            throw new ProvedorExternoException( "Falha funcional preservada.", new IOException( valor ) );
        }

        @GetMapping("/restrito")
        @PreAuthorize("hasRole('ACAO_INEXISTENTE')")
        void restrito() {
        }

        @GetMapping("/negocio")
        @PreAuthorize("isAuthenticated()")
        void negocio() {
            throw new BusinessException( "Regra de teste recusou a operação." );
        }

        @GetMapping("/inesperado")
        @PreAuthorize("isAuthenticated()")
        void inesperado() {
            throw new IllegalStateException( "segredo-interno" );
        }

        @GetMapping("/inesperado/{valor}")
        @PreAuthorize("isAuthenticated()")
        void inesperadoComEntrada( @PathVariable String valor ) {
            throw new IllegalStateException( valor );
        }

        @GetMapping("/uuid/{id}")
        @PreAuthorize("isAuthenticated()")
        UUID uuid( @PathVariable UUID id ) {
            return id;
        }
    }
}
