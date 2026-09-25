package br.com.gems.app.normalizacao.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.gems.app.model.dto.ContatoDTO;
import br.com.gems.app.model.entity.normalizacao.Contato;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ContatoMapperTest {
    private final ContatoMapper mapper = Mappers.getMapper( ContatoMapper.class );

    @Test
    void deveMapearCamposPublicos_quandoContatoInformado() {
        var id = UUID.randomUUID();
        var contato = new Contato();
        contato.setId( id );
        contato.setEmail( "ana@example.test" );
        contato.setTelefone( "11999998888" );
        contato.setDataCriacao( LocalDateTime.of( 2020, 1, 1, 0, 0 ) );
        contato.setDataExclusao( LocalDateTime.of( 2024, 1, 1, 0, 0 ) );
        var dto = mapper.toDTO( contato );
        assertEquals( id, dto.getId() );
        assertEquals( "ana@example.test", dto.getEmail() );
        assertEquals( "11999998888", dto.getTelefone() );
    }

    @Test
    void deveAtualizarCamposEditaveis_quandoEntidadeExistente() {
        var id = UUID.randomUUID();
        var criacao = LocalDateTime.of( 2020, 1, 1, 0, 0 );
        var exclusao = LocalDateTime.of( 2024, 1, 1, 0, 0 );
        var entidade = new Contato();
        entidade.setId( id );
        entidade.setDataCriacao( criacao );
        entidade.setDataExclusao( exclusao );
        var dto = new ContatoDTO( UUID.randomUUID(), "novo@example.test", "21988887777" );
        mapper.atualizar( dto, entidade );
        assertEquals( id, entidade.getId() );
        assertEquals( criacao, entidade.getDataCriacao() );
        assertEquals( exclusao, entidade.getDataExclusao() );
        assertEquals( dto.getEmail(), entidade.getEmail() );
        assertEquals( dto.getTelefone(), entidade.getTelefone() );
    }
}
