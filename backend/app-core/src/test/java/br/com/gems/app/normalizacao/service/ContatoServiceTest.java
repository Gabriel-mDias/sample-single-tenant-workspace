package br.com.gems.app.normalizacao.service;

import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.normalizacao.repository.ContatoRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobertura das regras V8 e V9 do {@code data-model.md} da feature 007-vinculos-de-servico.
 *
 * <p>O {@link ModelMapper} aqui é real, não mock: o mapeamento DTO↔entidade é parte da regra que se
 * quer proteger — em especial o {@code skip} de identidade, que impede o payload de sobrescrever o
 * id do contato já persistido.
 */
@ExtendWith( MockitoExtension.class )
@MockitoSettings( strictness = Strictness.LENIENT )
class ContatoServiceTest {

    private static final UUID ID_CONTATO = UUID.fromString( "11111111-1111-1111-1111-111111111111" );
    private static final UUID ID_OUTRO_CONTATO = UUID.fromString( "22222222-2222-2222-2222-222222222222" );

    @Mock
    private ContatoRepository repository;

    private ContatoService service;

    @BeforeEach
    void setUp() {
        var modelMapper = new ModelMapper();
        ModelMapperUtils.ignoreLazyFieldsNotInitialized( modelMapper );
        modelMapper.getConfiguration().setAmbiguityIgnored( true );

        service = new ContatoService( modelMapper, repository );

        when( repository.save( any( Contato.class ) ) ).thenAnswer( invocation -> invocation.getArgument( 0 ) );
    }

    @Test
    @DisplayName( "ContatoDTO nulo acusa que os dados de contato são obrigatórios" )
    void contatoNuloFalha() {
        var errors = new ArrayList<String>();

        service.validar( null, errors );

        assertThat( errors ).containsExactly( "Os dados de contato são obrigatórios." );
    }

    @Test
    @DisplayName( "V8 e V9: e-mail e telefone ausentes acumulam duas mensagens distintas" )
    void emailETelefoneAusentesAcumulamMensagens() {
        var errors = new ArrayList<String>();

        service.validar( new ContatoDTO(), errors );

        assertThat( errors ).containsExactlyInAnyOrder(
                "O e-mail é obrigatório.",
                "O telefone é obrigatório." );
    }

    @Test
    @DisplayName( "ContatoDTO válido não acumula nenhuma mensagem" )
    void contatoValidoNaoAcumulaMensagem() {
        var errors = new ArrayList<String>();

        service.validar( contatoValido(), errors );

        assertThat( errors ).isEmpty();
    }

    @Test
    @DisplayName( "insert devolve instância gerenciada" )
    void insertDevolveInstanciaGerenciada() {
        var salvo = service.insert( contatoValido() );

        assertThat( salvo.getEmail() ).isEqualTo( "consolato@esteri.it" );
        assertThat( salvo.getTelefone() ).isEqualTo( "+55 21 3534-1300" );
        verify( repository ).save( any( Contato.class ) );
    }

    @Test
    @DisplayName( "save sobre instância existente preserva o id, mesmo com id diferente no DTO" )
    void savePreservaId() {
        var existente = Contato.builder().id( ID_CONTATO ).email( "antigo@esteri.it" ).telefone( "0000" ).build();
        var dto = contatoValido();
        dto.setId( ID_OUTRO_CONTATO );

        var salvo = service.save( existente, dto );

        assertThat( salvo.getId() ).isEqualTo( ID_CONTATO );
        assertThat( salvo.getEmail() ).isEqualTo( "consolato@esteri.it" );
    }

    @Test
    @DisplayName( "delete com argumento nulo não estoura e não chama o repositório" )
    void deleteComArgumentoNuloNaoEstoura() {
        assertThatCode( () -> service.delete( null ) ).doesNotThrowAnyException();

        verify( repository, never() ).delete( any() );
    }

    private ContatoDTO contatoValido() {
        var dto = new ContatoDTO();
        dto.setEmail( "consolato@esteri.it" );
        dto.setTelefone( "+55 21 3534-1300" );
        return dto;
    }

}
