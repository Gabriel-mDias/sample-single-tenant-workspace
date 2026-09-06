package br.com.gems.app.normalizacao.service;

import br.com.gems.app.localidade.repository.EstadoRepository;
import br.com.gems.app.localidade.repository.PaisRepository;
import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.dto.EstadoDTO;
import br.com.gems.app.model.dto.PaisDTO;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.app.model.entity.normalizacao.Endereco;
import br.com.gems.app.normalizacao.repository.EnderecoRepository;
import br.com.gems.mapper.ModelMapperUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cobertura das regras V10 a V16 do {@code data-model.md} da feature 007-vinculos-de-servico, mais
 * as regras de país introduzidas pela 009.
 *
 * <p>O {@link ModelMapper} aqui é real, não mock: o mapeamento DTO↔entidade é parte da regra que se
 * quer proteger — em especial o {@code skip} de identidade, de {@code estado} e de {@code pais}, que
 * impedem o payload de sobrescrever o id do endereço e de adulterar dados de referência
 * compartilhados.
 */
@ExtendWith( MockitoExtension.class )
@MockitoSettings( strictness = Strictness.LENIENT )
class EnderecoServiceTest {

    private static final UUID ID_ENDERECO = UUID.fromString( "11111111-1111-1111-1111-111111111111" );
    private static final UUID ID_OUTRO_ENDERECO = UUID.fromString( "22222222-2222-2222-2222-222222222222" );
    private static final UUID ID_BRASIL = UUID.fromString( "33333333-3333-3333-3333-333333333333" );
    private static final UUID ID_PORTUGAL = UUID.fromString( "44444444-4444-4444-4444-444444444444" );
    private static final UUID ID_RJ = UUID.fromString( "55555555-5555-5555-5555-555555555555" );
    private static final UUID ID_LISBOA = UUID.fromString( "66666666-6666-6666-6666-666666666666" );

    @Mock
    private EnderecoRepository repository;

    @Mock
    private EstadoRepository estadoRepository;

    @Mock
    private PaisRepository paisRepository;

    private EnderecoService service;

    private Pais brasil;
    private Estado rioDeJaneiro;
    private Estado lisboa;

    @BeforeEach
    void setUp() {
        var modelMapper = new ModelMapper();
        ModelMapperUtils.ignoreLazyFieldsNotInitialized( modelMapper );
        modelMapper.getConfiguration().setAmbiguityIgnored( true );

        service = new EnderecoService( modelMapper, repository, estadoRepository, paisRepository );

        brasil = Pais.builder().id( ID_BRASIL ).nome( "Brasil" ).build();
        var portugal = Pais.builder().id( ID_PORTUGAL ).nome( "Portugal" ).build();
        rioDeJaneiro = Estado.builder().id( ID_RJ ).nome( "Rio de Janeiro" ).siglaEstado( "RJ" ).pais( brasil ).build();
        lisboa = Estado.builder().id( ID_LISBOA ).nome( "Lisboa" ).siglaEstado( "LI" ).pais( portugal ).build();

        when( paisRepository.findById( ID_BRASIL ) ).thenReturn( Optional.of( brasil ) );
        when( paisRepository.findById( ID_PORTUGAL ) ).thenReturn( Optional.of( portugal ) );
        when( estadoRepository.findById( ID_RJ ) ).thenReturn( Optional.of( rioDeJaneiro ) );
        when( estadoRepository.findById( ID_LISBOA ) ).thenReturn( Optional.of( lisboa ) );
        when( repository.save( any( Endereco.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
    }

    @Test
    @DisplayName( "EnderecoDTO nulo acusa que o endereço é obrigatório" )
    void enderecoNuloFalha() {
        var errors = new ArrayList<String>();

        service.validar( null, errors );

        assertThat( errors ).containsExactly( "O endereço é obrigatório." );
    }

    @Test
    @DisplayName( "V13 a V16 e V11: obrigatórios ausentes acumulam mensagens; número e complemento são opcionais" )
    void camposObrigatoriosAusentesAcumulamMensagens() {
        var errors = new ArrayList<String>();

        service.validar( new EnderecoDTO(), errors );

        assertThat( errors ).contains(
                "O país é obrigatório.",
                "O CEP é obrigatório.",
                "A cidade é obrigatória.",
                "O bairro é obrigatório.",
                "O logradouro é obrigatório.",
                "O estado é obrigatório." );
        assertThat( errors )
                .noneMatch( mensagem -> mensagem.toLowerCase().contains( "número" ) )
                .noneMatch( mensagem -> mensagem.toLowerCase().contains( "complemento" ) );
    }

    @Test
    @DisplayName( "país ausente é recusado" )
    void paisAusenteFalha() {
        var errors = new ArrayList<String>();
        var dto = enderecoBrasileiro();
        dto.setPais( null );

        service.validar( dto, errors );

        assertThat( errors ).contains( "O país é obrigatório." );
    }

    @Test
    @DisplayName( "país informado que não existe no repositório é recusado" )
    void paisInexistenteFalha() {
        var inexistente = UUID.randomUUID();
        when( paisRepository.findById( inexistente ) ).thenReturn( Optional.empty() );

        var errors = new ArrayList<String>();
        var dto = enderecoBrasileiro();
        dto.setPais( paisDTO( inexistente ) );

        service.validar( dto, errors );

        assertThat( errors ).contains( "O país informado não existe." );
    }

    @Test
    @DisplayName( "endereço no Brasil sem CEP é recusado" )
    void brasilSemCepFalha() {
        var errors = new ArrayList<String>();
        var dto = enderecoBrasileiro();
        dto.setCep( null );

        service.validar( dto, errors );

        assertThat( errors ).containsExactly( "O CEP é obrigatório." );
    }

    @Test
    @DisplayName( "endereço no Brasil sem estado é recusado" )
    void brasilSemEstadoFalha() {
        var errors = new ArrayList<String>();
        var dto = enderecoBrasileiro();
        dto.setEstado( null );

        service.validar( dto, errors );

        assertThat( errors ).containsExactly( "O estado é obrigatório." );
    }

    @Test
    @DisplayName( "endereço fora do Brasil dispensa CEP e estado, exigindo apenas cidade, bairro e logradouro" )
    void estrangeiroSemCepESemEstadoPassa() {
        var errors = new ArrayList<String>();
        var dto = new EnderecoDTO();
        dto.setPais( paisDTO( ID_PORTUGAL ) );
        dto.setCidade( "Lisboa" );
        dto.setBairro( "Trastevere" );
        dto.setLogradouro( "Via della Lungaretta" );

        service.validar( dto, errors );

        assertThat( errors ).isEmpty();
    }

    @Test
    @DisplayName( "fora do Brasil, cidade, bairro e logradouro continuam obrigatórios" )
    void estrangeiroSemCidadeFalha() {
        var errors = new ArrayList<String>();
        var dto = new EnderecoDTO();
        dto.setPais( paisDTO( ID_PORTUGAL ) );
        dto.setLogradouro( "Via della Lungaretta" );

        service.validar( dto, errors );

        assertThat( errors ).containsExactlyInAnyOrder(
                "A cidade é obrigatória.",
                "O bairro é obrigatório." );
    }

    @Test
    @DisplayName( "CHECAGEM NOVA: estado que não pertence ao país informado é recusado — impossível antes, "
            + "quando o país era derivado do próprio estado" )
    void estadoDeOutroPaisFalha() {
        var errors = new ArrayList<String>();
        var dto = enderecoBrasileiro();
        dto.setEstado( estadoDTO( ID_LISBOA ) );

        service.validar( dto, errors );

        assertThat( errors ).containsExactly( "O estado informado não pertence ao país selecionado." );
    }

    @Test
    @DisplayName( "país ausente NÃO relaxa CEP e estado: a relaxação exige a prova de um país que não é o Brasil" )
    void paisAusenteNaoRelaxaRegraBrasileira() {
        var errors = new ArrayList<String>();
        var dto = new EnderecoDTO();
        dto.setCidade( "Lisboa" );
        dto.setBairro( "Trastevere" );
        dto.setLogradouro( "Via della Lungaretta" );

        service.validar( dto, errors );

        assertThat( errors ).contains(
                "O país é obrigatório.",
                "O CEP é obrigatório.",
                "O estado é obrigatório." );
    }

    @Test
    @DisplayName( "V12: estado informado que não existe no repositório é recusado" )
    void estadoInexistenteFalha() {
        var inexistente = UUID.randomUUID();
        when( estadoRepository.findById( inexistente ) ).thenReturn( Optional.empty() );

        var errors = new ArrayList<String>();
        var dto = enderecoBrasileiro();
        dto.setEstado( estadoDTO( inexistente ) );

        service.validar( dto, errors );

        assertThat( errors ).containsExactly( "O estado informado não existe." );
    }

    @Test
    @DisplayName( "AUSÊNCIA DE TRAVA DE PAÍS: endereço fora do Brasil passa sem erro — a regra de "
            + "sede em território brasileiro é do ParceiroService, e não pode vazar para cá" )
    void enderecoComEstadoEstrangeiroPassaSemErro() {
        var errors = new ArrayList<String>();
        var dto = enderecoValido( ID_PORTUGAL, ID_LISBOA );

        service.validar( dto, errors );

        assertThat( errors ).isEmpty();
    }

    @Test
    @DisplayName( "endereço válido com estado brasileiro não acumula nenhuma mensagem" )
    void enderecoValidoNaoAcumulaMensagem() {
        var errors = new ArrayList<String>();

        service.validar( enderecoBrasileiro(), errors );

        assertThat( errors ).isEmpty();
    }

    @Test
    @DisplayName( "save resolve o Estado pelo EstadoRepository e usa a instância gerenciada" )
    void saveUsaEstadoGerenciado() {
        var salvo = service.save( new Endereco(), enderecoBrasileiro() );

        assertThat( salvo.getEstado() ).isSameAs( rioDeJaneiro );
    }

    @Test
    @DisplayName( "save resolve o País pelo PaisRepository e usa a instância gerenciada" )
    void saveUsaPaisGerenciado() {
        var salvo = service.save( new Endereco(), enderecoBrasileiro() );

        assertThat( salvo.getPais() ).isSameAs( brasil );
    }

    @Test
    @DisplayName( "save de endereço estrangeiro sem estado grava com estado nulo, sem estourar" )
    void saveEstrangeiroSemEstadoGravaComEstadoNulo() {
        var dto = new EnderecoDTO();
        dto.setPais( paisDTO( ID_PORTUGAL ) );
        dto.setCidade( "Lisboa" );
        dto.setBairro( "Trastevere" );
        dto.setLogradouro( "Via della Lungaretta" );

        var salvo = service.save( new Endereco(), dto );

        assertThat( salvo.getEstado() ).isNull();
        assertThat( salvo.getPais().getNome() ).isEqualTo( "Portugal" );
    }

    @Test
    @DisplayName( "save não adultera o Estado de referência mesmo com nome e sigla alterados no payload" )
    void saveNaoAdulteraEstadoDeReferencia() {
        var dto = enderecoBrasileiro();
        dto.getEstado().setNome( "Rio de Janeiro ADULTERADO" );
        dto.getEstado().setSiglaEstado( "XX" );

        service.save( new Endereco(), dto );

        assertThat( rioDeJaneiro.getNome() ).isEqualTo( "Rio de Janeiro" );
        assertThat( rioDeJaneiro.getSiglaEstado() ).isEqualTo( "RJ" );
    }

    @Test
    @DisplayName( "save não adultera o País de referência mesmo com nome alterado no payload" )
    void saveNaoAdulteraPaisDeReferencia() {
        var dto = enderecoBrasileiro();
        dto.getPais().setNome( "Brasil ADULTERADO" );

        service.save( new Endereco(), dto );

        assertThat( brasil.getNome() ).isEqualTo( "Brasil" );
    }

    @Test
    @DisplayName( "save sobre instância existente preserva o id, mesmo com id diferente no DTO" )
    void savePreservaId() {
        var existente = Endereco.builder().id( ID_ENDERECO ).pais( brasil ).estado( rioDeJaneiro ).build();
        var dto = enderecoBrasileiro();
        dto.setId( ID_OUTRO_ENDERECO );

        var salvo = service.save( existente, dto );

        assertThat( salvo.getId() ).isEqualTo( ID_ENDERECO );
    }

    @Test
    @DisplayName( "buscarEstado devolve vazio para EnderecoDTO nulo" )
    void buscarEstadoComDtoNuloDevolveVazio() {
        assertThat( service.buscarEstado( null ) ).isEmpty();
    }

    @Test
    @DisplayName( "buscarEstado devolve vazio quando o estado do DTO é nulo" )
    void buscarEstadoComEstadoNuloDevolveVazio() {
        var dto = enderecoBrasileiro();
        dto.setEstado( null );

        assertThat( service.buscarEstado( dto ) ).isEmpty();
    }

    @Test
    @DisplayName( "buscarEstado devolve vazio quando o id do estado do DTO é nulo" )
    void buscarEstadoComIdDoEstadoNuloDevolveVazio() {
        var dto = enderecoBrasileiro();
        dto.getEstado().setId( null );

        assertThat( service.buscarEstado( dto ) ).isEmpty();
    }

    @Test
    @DisplayName( "buscarPais devolve vazio para EnderecoDTO nulo, país nulo ou id nulo" )
    void buscarPaisDevolveVazioNosCasosDegenerados() {
        var semPais = enderecoBrasileiro();
        semPais.setPais( null );

        var semId = enderecoBrasileiro();
        semId.getPais().setId( null );

        assertThat( service.buscarPais( null ) ).isEmpty();
        assertThat( service.buscarPais( semPais ) ).isEmpty();
        assertThat( service.buscarPais( semId ) ).isEmpty();
    }

    private EnderecoDTO enderecoBrasileiro() {
        return enderecoValido( ID_BRASIL, ID_RJ );
    }

    private EnderecoDTO enderecoValido( UUID idPais, UUID idEstado ) {
        var dto = new EnderecoDTO();
        dto.setPais( paisDTO( idPais ) );
        dto.setEstado( estadoDTO( idEstado ) );
        dto.setCep( "22250-040" );
        dto.setCidade( "Rio de Janeiro" );
        dto.setBairro( "Botafogo" );
        dto.setLogradouro( "Praia do Flamengo" );
        dto.setNumero( "200" );
        return dto;
    }

    private EstadoDTO estadoDTO( UUID id ) {
        var dto = new EstadoDTO();
        dto.setId( id );
        return dto;
    }

    private PaisDTO paisDTO( UUID id ) {
        var dto = new PaisDTO();
        dto.setId( id );
        return dto;
    }

}
