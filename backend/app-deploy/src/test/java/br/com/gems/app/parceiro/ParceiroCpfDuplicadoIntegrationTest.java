package br.com.gems.app.parceiro;

import br.com.gems.app.localidade.service.LocalidadeService;
import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.dto.ParceiroDTO;
import br.com.gems.app.model.dto.PessoaDTO;
import br.com.gems.app.parceiro.service.ParceiroService;
import br.com.gems.app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regressão HTTP do ID de pessoa forjado no corpo da edição. */
@AutoConfigureMockMvc
class ParceiroCpfDuplicadoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocalidadeService localidadeService;

    @Autowired
    private ParceiroService parceiroService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule( new JavaTimeModule() );

    @Test
    @Transactional
    void deveRecusarCpfDeOutraPessoa_quandoCorpoForjaIdDaPessoa() throws Exception {
        var sufixo = UUID.randomUUID().toString();
        var seed = Math.floorMod( System.nanoTime(), 99999999998L );
        var cpfOriginal = String.format( "%011d", seed );
        var cpfAlheio = String.format( "%011d", seed + 1 );
        var primeiro = criar( payload( "primeiro-" + sufixo, cpfOriginal ) );
        var segundo = criar( payload( "segundo-" + sufixo, cpfAlheio ) );
        var edicao = payload( "primeiro-" + sufixo, cpfAlheio );
        edicao.getPessoa().setId( segundo.getPessoa().getId() );

        mockMvc.perform( put( "/api/parceiros/{id}", primeiro.getId() )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_ALTERAR_PARCEIRO" ) ) )
                .contentType( MediaType.APPLICATION_JSON )
                .content( objectMapper.writeValueAsString( edicao ) ) )
                .andExpect( status().isBadRequest() )
                .andExpect( jsonPath( "$.message" ).value(
                        org.hamcrest.Matchers.containsString( "Já existe uma pessoa cadastrada com este CPF." ) ) );

        assertThat( parceiroService.findById( primeiro.getId() ).getPessoa().getCpf() ).isEqualTo( cpfOriginal );
        assertThat( parceiroService.findById( segundo.getId() ).getPessoa().getCpf() ).isEqualTo( cpfAlheio );
    }

    private ParceiroDTO criar( ParceiroDTO dto ) throws Exception {
        var resposta = mockMvc.perform( post( "/api/parceiros" )
                .with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_INSERIR_PARCEIRO" ) ) )
                .contentType( MediaType.APPLICATION_JSON )
                .content( objectMapper.writeValueAsString( dto ) ) )
                .andExpect( status().isCreated() )
                .andReturn();
        return objectMapper.readValue( resposta.getResponse().getContentAsString(), ParceiroDTO.class );
    }

    private ParceiroDTO payload( String nome, String cpf ) {
        var pessoa = new PessoaDTO();
        pessoa.setPrimeiroNome( "Pessoa" );
        pessoa.setSobrenome( nome );
        pessoa.setDataNascimento( LocalDate.of( 1990, 1, 1 ) );
        pessoa.setCpf( cpf );
        var contato = new ContatoDTO();
        contato.setEmail( nome + "@example.test" );
        contato.setTelefone( "11999999999" );
        var endereco = new EnderecoDTO();
        endereco.setPais( localidadeService.listarPaises().stream()
                .filter( pais -> "Brasil".equalsIgnoreCase( pais.getNome() ) ).findFirst().orElseThrow() );
        endereco.setEstado( localidadeService.listarEstadosPorPais( endereco.getPais().getId() ).getFirst() );
        endereco.setCep( "01001000" );
        endereco.setCidade( "São Paulo" );
        endereco.setBairro( "Centro" );
        endereco.setLogradouro( "Rua de Teste" );
        var dto = new ParceiroDTO();
        dto.setNome( nome );
        dto.setPessoa( pessoa );
        dto.setContato( contato );
        dto.setEndereco( endereco );
        return dto;
    }
}
