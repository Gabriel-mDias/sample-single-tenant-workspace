package br.com.gems.app.commons.response;

import br.com.gems.app.auditoria.controller.AuditoriaController;
import br.com.gems.app.auditoria.service.AuditoriaService;
import br.com.gems.app.model.response.ParceiroResponseDTO;
import br.com.gems.app.model.response.RegistroAuditoriaResponseDTO;
import br.com.gems.app.model.response.SolicitacaoResponseDTO;
import br.com.gems.app.parceiro.controller.ParceiroController;
import br.com.gems.app.parceiro.service.ParceiroService;
import br.com.gems.app.solicitacao.controller.SolicitacaoController;
import br.com.gems.app.solicitacao.service.SolicitacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuscaPaginadaControllerTest {

    @Test
    void deveExporContratoEstavel_quandoBuscaNasQuatroRotasPaginadas() throws Exception {
        // Arrange
        var parceiro = mock( ParceiroService.class );
        var solicitacao = mock( SolicitacaoService.class );
        var auditoria = mock( AuditoriaService.class );
        when( parceiro.search( any(), any() ) ).thenReturn( new PageImpl<ParceiroResponseDTO>(
                List.of(), PageRequest.of( 2, 10 ), 21 ) );
        when( solicitacao.search( any(), any() ) ).thenReturn( new PageImpl<SolicitacaoResponseDTO>(
                List.of(), PageRequest.of( 2, 10 ), 21 ) );
        var paginaAuditoria = new PageImpl<RegistroAuditoriaResponseDTO>( List.of(), PageRequest.of( 2, 10 ), 21 );
        when( auditoria.search( any(), any() ) ).thenReturn( paginaAuditoria );
        when( auditoria.historico( anyString(), any(), any() ) ).thenReturn( paginaAuditoria );
        MockMvc mvc = MockMvcBuilders.standaloneSetup( new ParceiroController( parceiro ),
                new SolicitacaoController( solicitacao ), new AuditoriaController( auditoria ) )
                .setCustomArgumentResolvers( new PageableHandlerMethodArgumentResolver() ).build();

        // Act + Assert
        verificarPagina( mvc, "/api/parceiros/search" );
        verificarPagina( mvc, "/api/solicitacoes/search" );
        verificarPagina( mvc, "/api/auditoria/search" );
        mvc.perform( get( "/api/auditoria/parceiro/" + UUID.randomUUID() ).param( "page", "2" )
                .param( "size", "10" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.totalElements" ).value( 21 ) )
                .andExpect( jsonPath( "$.totalPages" ).value( 3 ) )
                .andExpect( jsonPath( "$.number" ).value( 2 ) )
                .andExpect( jsonPath( "$.size" ).value( 10 ) )
                .andExpect( jsonPath( "$.pageable" ).doesNotExist() );
    }

    private void verificarPagina( MockMvc mvc, String rota ) throws Exception {
        mvc.perform( post( rota ).param( "page", "2" ).param( "size", "10" )
                .contentType( MediaType.APPLICATION_JSON ).content( "{}" ) )
                .andExpect( status().isOk() )
                .andExpect( jsonPath( "$.content" ).isArray() )
                .andExpect( jsonPath( "$.totalElements" ).value( 21 ) )
                .andExpect( jsonPath( "$.totalPages" ).value( 3 ) )
                .andExpect( jsonPath( "$.number" ).value( 2 ) )
                .andExpect( jsonPath( "$.size" ).value( 10 ) )
                .andExpect( jsonPath( "$.pageable" ).doesNotExist() );
    }

}
