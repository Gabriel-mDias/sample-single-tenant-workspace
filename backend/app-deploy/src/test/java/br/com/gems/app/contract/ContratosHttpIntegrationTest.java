package br.com.gems.app.contract;

import br.com.gems.app.localidade.service.LocalidadeService;
import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.dto.ParceiroDTO;
import br.com.gems.app.model.dto.PessoaDTO;
import br.com.gems.app.model.dto.SolicitacaoDTO;
import br.com.gems.app.support.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Contratos HTTP próprios com banco PostgreSQL real e provedor CEP local determinístico. */
@AutoConfigureMockMvc
class ContratosHttpIntegrationTest extends AbstractIntegrationTest {

    private static final HttpServer CEP = iniciarCep();
    private static final ObjectMapper JSON = new ObjectMapper().registerModule( new JavaTimeModule() );
    private static final AtomicLong SEQUENCIA = new AtomicLong( 1000 );

    @Autowired
    MockMvc mockMvc;
    @Autowired
    LocalidadeService localidades;

    @DynamicPropertySource
    static void cepUrl( DynamicPropertyRegistry registry ) {
        registry.add( "app.cep.viacep.url", () -> "http://127.0.0.1:" + CEP.getAddress().getPort() );
    }

    @AfterAll
    static void pararCep() {
        CEP.stop( 0 );
    }

    @Test
    @Transactional
    void deveCobrirCadastroBuscaEdicaoEExclusaoDeParceiro() throws Exception {
        var nome = nomeUnico( "parceiro" );
        var corpo = parceiro( nome );
        var criado = chamar( post( "/api/parceiros" ), "INSERIR_PARCEIRO", 201, corpo );
        var id = criado.path( "id" ).asText();
        assertThat( criado.path( "nome" ).asText() ).isEqualTo( nome );
        assertThat( criado.path( "pessoa" ).path( "cpf" ).asText() ).isEqualTo( corpo.getPessoa().getCpf() );
        assertThat( criado.path( "contato" ).path( "email" ).asText() ).isEqualTo( corpo.getContato().getEmail() );

        var lido = chamar( get( "/api/parceiros/{id}", id ), "CONSULTAR_PARCEIRO", 200, null );
        assertThat( lido.path( "id" ).asText() ).isEqualTo( id );
        assertThat( lido.path( "endereco" ).path( "cidade" ).asText() ).isEqualTo( "São Paulo" );

        var busca = chamar( post( "/api/parceiros/search" ).param( "page", "0" ).param( "size", "10" ),
                "CONSULTAR_PARCEIRO", 200, "{\"nome\":\"" + nome + "\",\"cpf\":\""
                        + corpo.getPessoa().getCpf() + "\",\"estadoId\":\""
                        + corpo.getEndereco().getEstado().getId() + "\"}" );
        assertThat( busca.path( "totalElements" ).asLong() ).isEqualTo( 1 );
        assertThat( busca.path( "content" ).get( 0 ).path( "nome" ).asText() ).isEqualTo( nome );
        assertThat( busca.path( "content" ).get( 0 ).path( "cpf" ).asText() )
                .isEqualTo( corpo.getPessoa().getCpf() );
        assertThat( busca.path( "content" ).get( 0 ).path( "email" ).asText() )
                .isEqualTo( corpo.getContato().getEmail() );
        assertThat( busca.path( "content" ).get( 0 ).path( "estadoSigla" ).asText() ).isEqualTo( "SP" );
        var outroId = chamar( post( "/api/parceiros/search" ), "CONSULTAR_PARCEIRO", 200,
                "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"nome\":\"" + nome + "\"}" );
        assertThat( outroId.path( "totalElements" ).asLong() ).isZero();

        corpo.setNome( nome + "-editado" );
        var editado = chamar( put( "/api/parceiros/{id}", id ), "ALTERAR_PARCEIRO", 200, corpo );
        assertThat( editado.path( "nome" ).asText() ).isEqualTo( corpo.getNome() );

        chamar( delete( "/api/parceiros/{id}", id ), "EXCLUIR_PARCEIRO", 204, null );
        var ausente = chamar( get( "/api/parceiros/{id}", id ), "CONSULTAR_PARCEIRO", 400, null );
        assertThat( ausente.path( "message" ).asText() ).isNotBlank();
    }

    @Test
    @Transactional
    void deveRejeitarCadastroEEdicaoInvalidosDeParceiro() throws Exception {
        var vazio = chamar( post( "/api/parceiros" ), "INSERIR_PARCEIRO", 400, new ParceiroDTO() );
        assertThat( vazio.path( "message" ).asText() ).isNotBlank();
        var id = chamar( post( "/api/parceiros" ), "INSERIR_PARCEIRO", 201,
                parceiro( nomeUnico( "invalido" ) ) ).path( "id" ).asText();
        var edicao = chamar( put( "/api/parceiros/{id}", id ), "ALTERAR_PARCEIRO", 400,
                new ParceiroDTO() );
        assertThat( edicao.path( "message" ).asText() ).isNotBlank();
        assertThat( chamar( delete( "/api/parceiros/{id}", UUID.randomUUID() ),
                "EXCLUIR_PARCEIRO", 400, null ).path( "message" ).asText() ).isNotBlank();
        var filtroInvalido = chamar( post( "/api/parceiros/search" ), "CONSULTAR_PARCEIRO", 400,
                "{\"estadoId\":\"invalido\"}" );
        assertThat( filtroInvalido.path( "codigo" ).asText() ).isEqualTo( "CORPO_INVALIDO" );
        assertThat( filtroInvalido.path( "message" ).asText() ).doesNotContain( "invalido" );
    }

    @Test
    @Transactional
    void deveCobrirCadastroBuscaEdicaoEExclusaoDeSolicitacao() throws Exception {
        var parceiroId = criarParceiro();
        var titulo = nomeUnico( "solicitacao" );
        var payload = solicitacao( parceiroId, titulo );
        payload.setSituacao( br.com.gems.app.model.enums.SituacaoSolicitacao.CONCLUIDA );
        var criada = chamar( post( "/api/solicitacoes" ), "INSERIR_SOLICITACAO", 201, payload );
        var id = criada.path( "id" ).asText();
        assertThat( criada.path( "situacao" ).asText() ).isEqualTo( "RASCUNHO" );
        assertThat( criada.path( "parceiroId" ).asText() ).isEqualTo( parceiroId );
        assertThat( chamar( get( "/api/solicitacoes/{id}", id ), "CONSULTAR_SOLICITACAO", 200, null )
                .path( "titulo" ).asText() ).isEqualTo( titulo );
        var busca = chamar( post( "/api/solicitacoes/search" ), "CONSULTAR_SOLICITACAO", 200,
                "{\"id\":\"" + id + "\",\"titulo\":\"" + titulo
                        + "\",\"situacao\":\"RASCUNHO\",\"parceiroId\":\"" + parceiroId + "\"}" );
        assertThat( busca.path( "totalElements" ).asLong() ).isEqualTo( 1 );
        assertThat( busca.path( "content" ).get( 0 ).path( "titulo" ).asText() ).isEqualTo( titulo );
        assertThat( busca.path( "content" ).get( 0 ).path( "parceiroId" ).asText() ).isEqualTo( parceiroId );
        assertThat( busca.path( "content" ).get( 0 ).path( "situacao" ).asText() ).isEqualTo( "RASCUNHO" );
        var outroId = chamar( post( "/api/solicitacoes/search" ), "CONSULTAR_SOLICITACAO", 200,
                "{\"id\":\"00000000-0000-0000-0000-000000000001\",\"titulo\":\"" + titulo + "\"}" );
        assertThat( outroId.path( "totalElements" ).asLong() ).isZero();
        payload.setTitulo( titulo + "-editado" );
        assertThat( chamar( put( "/api/solicitacoes/{id}", id ), "ALTERAR_SOLICITACAO", 200, payload )
                .path( "situacao" ).asText() ).isEqualTo( "RASCUNHO" );
        chamar( delete( "/api/solicitacoes/{id}", id ), "EXCLUIR_SOLICITACAO", 204, null );
        assertThat( chamar( get( "/api/solicitacoes/{id}", id ), "CONSULTAR_SOLICITACAO", 400, null )
                .path( "message" ).asText() ).isNotBlank();
    }

    @Test
    @Transactional
    void deveRejeitarEntradasInvalidasDeSolicitacao() throws Exception {
        assertThat( chamar( post( "/api/solicitacoes" ), "INSERIR_SOLICITACAO", 400,
                new SolicitacaoDTO() ).path( "message" ).asText() ).isNotBlank();
        var id = criarSolicitacao();
        assertThat( chamar( put( "/api/solicitacoes/{id}", id ), "ALTERAR_SOLICITACAO", 400,
                new SolicitacaoDTO() ).path( "message" ).asText() ).isNotBlank();
        var filtroInvalido = chamar( post( "/api/solicitacoes/search" ), "CONSULTAR_SOLICITACAO", 400,
                "{\"situacao\":\"DESCONHECIDA\"}" );
        assertThat( filtroInvalido.path( "codigo" ).asText() ).isEqualTo( "CORPO_INVALIDO" );
        assertThat( filtroInvalido.path( "message" ).asText() ).doesNotContain( "DESCONHECIDA" );
    }

    @Test
    @Transactional
    void deveExercitarOrigemValidaEInvalidaDeCadaTransicao() throws Exception {
        var id = criarSolicitacao();
        assertSituacao( post( "/api/solicitacoes/{id}/iniciar", id ), "INICIAR_SOLICITACAO", 400, null, null );
        assertSituacao( post( "/api/solicitacoes/{id}/concluir", id ), "CONCLUIR_SOLICITACAO", 400, null, null );
        assertSituacao( post( "/api/solicitacoes/{id}/abrir", id ), "ABRIR_SOLICITACAO", 200, null, "ABERTA" );
        assertSituacao( post( "/api/solicitacoes/{id}/abrir", id ), "ABRIR_SOLICITACAO", 400, null, null );
        assertSituacao( post( "/api/solicitacoes/{id}/iniciar", id ), "INICIAR_SOLICITACAO", 200, null,
                "EM_ANDAMENTO" );
        assertSituacao( post( "/api/solicitacoes/{id}/concluir", id ), "CONCLUIR_SOLICITACAO", 200, null, "CONCLUIDA" );
        assertSituacao( post( "/api/solicitacoes/{id}/concluir", id ), "CONCLUIR_SOLICITACAO", 400, null, null );
        assertThat( chamar( delete( "/api/solicitacoes/{id}", id ), "EXCLUIR_SOLICITACAO", 400, null )
                .path( "message" ).asText() ).isNotBlank();
        assertSituacao( post( "/api/solicitacoes/{id}/cancelar", id ), "CANCELAR_SOLICITACAO", 400,
                "{\"motivo\":\"motivo\"}", null );

        var outro = criarSolicitacao();
        assertSituacao( post( "/api/solicitacoes/{id}/cancelar", outro ), "CANCELAR_SOLICITACAO", 400,
                "{\"motivo\":\"\"}", null );
        var cancelada = chamar( post( "/api/solicitacoes/{id}/cancelar", outro ), "CANCELAR_SOLICITACAO", 200,
                "{\"motivo\":\"Contrato cancelado\"}" );
        assertThat( cancelada.path( "situacao" ).asText() ).isEqualTo( "CANCELADA" );
        assertThat( cancelada.path( "motivoCancelamento" ).asText() ).isEqualTo( "Contrato cancelado" );
    }

    @Test
    void deveCobrirListasDeLocalidadeEConversaoInvalida() throws Exception {
        var paises = chamar( get( "/api/localidades/paises" ), null, 200, null );
        assertThat( paises.isArray() ).isTrue();
        var brasil = localidades.listarPaises().stream().filter( p -> "Brasil".equalsIgnoreCase( p.getNome() ) )
                .findFirst().orElseThrow();
        assertThat( paises.toString() ).contains( brasil.getId().toString() );
        var estados = chamar( get( "/api/localidades/paises/{id}/estados", brasil.getId() ), null, 200, null );
        assertThat( estados.isArray() ).isTrue();
        assertThat( estados.size() ).isGreaterThan( 0 );
        assertThat( estados.get( 0 ).path( "siglaEstado" ).asText() ).isNotBlank();
        assertThat( chamar( get( "/api/localidades/paises/invalido/estados" ), null, 400, null )
                .path( "message" ).asText() ).isNotBlank();
    }

    @Test
    void deveCobrirCepValidoInexistenteEProvedorIndisponivel() throws Exception {
        var endereco = chamar( get( "/api/enderecos/cep/01001000" ), null, 200, null );
        assertThat( endereco.path( "cidade" ).asText() ).isEqualTo( "São Paulo" );
        assertThat( endereco.path( "estado" ).path( "siglaEstado" ).asText() ).isEqualTo( "SP" );
        assertThat( endereco.path( "numero" ).isNull() ).isTrue();
        assertThat( chamar( get( "/api/enderecos/cep/invalido" ), null, 400, null )
                .path( "message" ).asText() ).isNotBlank();
        assertThat( chamar( get( "/api/enderecos/cep/00000000" ), null, 400, null )
                .path( "message" ).asText() ).contains( "CEP" );
        assertThat( chamar( get( "/api/enderecos/cep/99999999" ), null, 502, null )
                .path( "message" ).asText() ).isNotBlank();
    }

    @Test
    void deveCobrirHistoricoBuscaEFiltrosInvalidosDeAuditoria() throws Exception {
        var id = criarParceiro();
        var historico = chamar( get( "/api/auditoria/Parceiro/{id}", id ), "CONSULTAR_AUDITORIA", 200, null );
        assertThat( historico.path( "content" ).isArray() ).isTrue();
        assertThat( historico.path( "totalElements" ).asLong() ).isGreaterThan( 0 );
        assertThat( historico.path( "content" ).get( 0 ).path( "id" ).asText() ).isNotBlank();
        assertThat( historico.path( "content" ).get( 0 ).path( "operacao" ).asText() ).isNotBlank();
        var busca = chamar( post( "/api/auditoria/search" ), "CONSULTAR_AUDITORIA", 200,
                "{\"entidade\":\"Parceiro\",\"registroAfetado\":\"" + id + "\"}" );
        assertThat( busca.path( "content" ).isArray() ).isTrue();
        assertThat( busca.path( "totalElements" ).asLong() ).isGreaterThan( 0 );
        assertThat( busca.path( "content" ).get( 0 ).path( "registroAfetado" ).asText() )
                .isEqualTo( id );
        assertThat( busca.path( "content" ).get( 0 ).path( "entidade" ).asText() ).isEqualTo( "Parceiro" );
        assertThat( busca.path( "content" ).get( 0 ).path( "dataOperacao" ).asText() ).isNotBlank();
        assertThat( chamar( get( "/api/auditoria/Desconhecida/{id}", id ), "CONSULTAR_AUDITORIA", 400, null )
                .path( "message" ).asText() ).isNotBlank();
        assertThat( chamar( post( "/api/auditoria/search" ), "CONSULTAR_AUDITORIA", 400,
                "{\"dataInicio\":\"2026-09-25\",\"dataFim\":\"2026-09-24\"}" )
                .path( "message" ).asText() ).isNotBlank();
        assertThat( chamar( post( "/api/auditoria/search" ), "CONSULTAR_AUDITORIA", 400,
                "{\"operacao\":\"DESCONHECIDA\"}" ).path( "message" ).asText() )
                .contains( "operação" );
    }

    private void assertSituacao( MockHttpServletRequestBuilder request, String acao, int status,
            Object corpo, String esperada ) throws Exception {
        var resposta = chamar( request, acao, status, corpo );
        assertThat( resposta.path( esperada == null ? "message" : "situacao" ).asText() )
                .isNotBlank();
        if ( esperada != null )
            assertThat( resposta.path( "situacao" ).asText() ).isEqualTo( esperada );
    }

    private JsonNode chamar( MockHttpServletRequestBuilder request, String acao, int status, Object corpo )
            throws Exception {
        if ( acao != null )
            request.with( jwt().authorities( new SimpleGrantedAuthority( "ROLE_" + acao ) ) );
        else
            request.with( jwt().authorities() );
        if ( corpo != null )
            request.contentType( MediaType.APPLICATION_JSON )
                    .content( corpo instanceof String s ? s : JSON.writeValueAsString( corpo ) );
        var response = mockMvc.perform( request ).andReturn().getResponse();
        assertThat( response.getStatus() ).as( request.toString() + " body=" + response.getContentAsString() )
                .isEqualTo( status );
        var bytes = response.getContentAsByteArray();
        if ( status == 204 )
            assertThat( bytes ).isEmpty();
        return bytes.length == 0 ? JSON.createObjectNode() : JSON.readTree( bytes );
    }

    private String criarParceiro() throws Exception {
        return chamar( post( "/api/parceiros" ), "INSERIR_PARCEIRO", 201,
                parceiro( nomeUnico( "fixture" ) ) ).path( "id" ).asText();
    }

    private String criarSolicitacao() throws Exception {
        return chamar( post( "/api/solicitacoes" ), "INSERIR_SOLICITACAO", 201,
                solicitacao( criarParceiro(), nomeUnico( "fixture" ) ) ).path( "id" ).asText();
    }

    private SolicitacaoDTO solicitacao( String parceiroId, String titulo ) {
        var dto = new SolicitacaoDTO();
        dto.setTitulo( titulo );
        dto.setDescricao( "Descrição de contrato" );
        dto.setParceiroId( UUID.fromString( parceiroId ) );
        return dto;
    }

    private ParceiroDTO parceiro( String nome ) {
        var pessoa = new PessoaDTO();
        pessoa.setPrimeiroNome( "Pessoa" );
        pessoa.setSobrenome( nome );
        pessoa.setDataNascimento( LocalDate.of( 1990, 1, 1 ) );
        pessoa.setCpf( String.format( "%011d", SEQUENCIA.incrementAndGet() ) );
        var contato = new ContatoDTO();
        contato.setEmail( nome + "@example.test" );
        contato.setTelefone( "11999999999" );
        var endereco = new EnderecoDTO();
        endereco.setPais( localidades.listarPaises().stream()
                .filter( p -> "Brasil".equalsIgnoreCase( p.getNome() ) ).findFirst().orElseThrow() );
        endereco.setEstado( localidades.listarEstadosPorPais( endereco.getPais().getId() ).stream()
                .filter( e -> "SP".equals( e.getSiglaEstado() ) ).findFirst().orElseThrow() );
        endereco.setCep( "01001000" );
        endereco.setCidade( "São Paulo" );
        endereco.setBairro( "Centro" );
        endereco.setLogradouro( "Rua de Teste" );
        var dto = new ParceiroDTO();
        dto.setNome( nome );
        dto.setDescricao( "Descrição de contrato" );
        dto.setComissao( new BigDecimal( "12.50" ) );
        dto.setPessoa( pessoa );
        dto.setContato( contato );
        dto.setEndereco( endereco );
        return dto;
    }

    private static HttpServer iniciarCep() {
        try {
            var server = HttpServer.create( new InetSocketAddress( "127.0.0.1", 0 ), 0 );
            server.createContext( "/", exchange -> {
                var path = exchange.getRequestURI().getPath();
                int status = path.contains( "99999999" ) ? 503 : 200;
                var body = path.contains( "00000000" )
                        ? "{\"erro\":true}"
                        : "{\"cep\":\"01001-000\",\"logradouro\":\"Praça da Sé\",\"bairro\":\"Sé\",\"localidade\":\"São Paulo\",\"uf\":\"SP\"}";
                var bytes = body.getBytes( StandardCharsets.UTF_8 );
                exchange.getResponseHeaders().add( "Content-Type", "application/json" );
                exchange.sendResponseHeaders( status, bytes.length );
                try ( var output = exchange.getResponseBody() ) {
                    output.write( bytes );
                }
            } );
            server.start();
            return server;
        } catch ( IOException ex ) {
            throw new ExceptionInInitializerError( ex );
        }
    }

    private static String nomeUnico( String tipo ) {
        return "Contrato-" + tipo + "-" + SEQUENCIA.incrementAndGet();
    }
}
