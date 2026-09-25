package br.com.gems.app.commons.response;

import br.com.gems.app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BuscaPaginadaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveResponderComCincoCampos_quandoConsultaPaginadaAutorizada() throws Exception {
        // Arrange
        var parceiro = new SimpleGrantedAuthority( "ROLE_CONSULTAR_PARCEIRO" );
        var solicitacao = new SimpleGrantedAuthority( "ROLE_CONSULTAR_SOLICITACAO" );
        var auditoria = new SimpleGrantedAuthority( "ROLE_CONSULTAR_AUDITORIA" );

        // Act + Assert
        verificarPagina( mockMvc.perform( post( "/api/parceiros/search" ).param( "page", "2" )
                .param( "size", "10" ).with( jwt().authorities( parceiro ) )
                .contentType( MediaType.APPLICATION_JSON ).content( "{}" ) ) );
        verificarPagina( mockMvc.perform( post( "/api/solicitacoes/search" ).param( "page", "2" )
                .param( "size", "10" ).with( jwt().authorities( solicitacao ) )
                .contentType( MediaType.APPLICATION_JSON ).content( "{}" ) ) );
        verificarPagina( mockMvc.perform( post( "/api/auditoria/search" ).param( "page", "2" )
                .param( "size", "10" ).with( jwt().authorities( auditoria ) )
                .contentType( MediaType.APPLICATION_JSON ).content( "{}" ) ) );
        verificarPagina( mockMvc.perform( get( "/api/auditoria/Parceiro/{id}", UUID.randomUUID() )
                .param( "page", "2" ).param( "size", "10" ).with( jwt().authorities( auditoria ) ) ) );
    }

    private void verificarPagina( ResultActions resultado ) throws Exception {
        var corpo = resultado.andExpect( status().isOk() ).andReturn().getResponse().getContentAsByteArray();
        var json = new ObjectMapper().readTree( corpo );

        assertThat( json.size() ).isEqualTo( 5 );
        assertThat( json.get( "content" ).isArray() ).isTrue();
        assertThat( json.get( "totalElements" ).isNumber() ).isTrue();
        assertThat( json.get( "totalPages" ).isNumber() ).isTrue();
        assertThat( json.get( "number" ).asInt() ).isEqualTo( 2 );
        assertThat( json.get( "size" ).asInt() ).isEqualTo( 10 );
    }

}
