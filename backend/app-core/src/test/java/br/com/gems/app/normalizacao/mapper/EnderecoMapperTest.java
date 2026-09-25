package br.com.gems.app.normalizacao.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import br.com.gems.app.model.dto.EnderecoDTO;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.app.model.entity.normalizacao.Endereco;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class EnderecoMapperTest {
    private final EnderecoMapper mapper = Mappers.getMapper( EnderecoMapper.class );

    @Test
    void deveMapearTodosOsCamposPublicosParaDTO() {
        var id = UUID.randomUUID();
        var paisId = UUID.randomUUID();
        var estadoId = UUID.randomUUID();
        var pais = Pais.builder().id( paisId ).nome( "Brasil" ).build();
        var estado = Estado.builder().id( estadoId ).pais( pais ).nome( "São Paulo" ).siglaEstado( "SP" ).build();
        var endereco = new Endereco();
        endereco.setId( id );
        endereco.setPais( pais );
        endereco.setEstado( estado );
        endereco.setCep( "01001000" );
        endereco.setCidade( "São Paulo" );
        endereco.setBairro( "Sé" );
        endereco.setLogradouro( "Praça da Sé" );
        endereco.setNumero( "1" );
        endereco.setComplemento( "sala 2" );
        endereco.setDataCriacao( LocalDateTime.of( 2020, 1, 1, 0, 0 ) );
        endereco.setDataExclusao( LocalDateTime.of( 2024, 1, 1, 0, 0 ) );
        var dto = mapper.toDTO( endereco );
        assertEquals( id, dto.getId() );
        assertEquals( paisId, dto.getPais().getId() );
        assertEquals( "Brasil", dto.getPais().getNome() );
        assertEquals( estadoId, dto.getEstado().getId() );
        assertEquals( "São Paulo", dto.getEstado().getNome() );
        assertEquals( "SP", dto.getEstado().getSiglaEstado() );
        assertEquals( "01001000", dto.getCep() );
        assertEquals( "São Paulo", dto.getCidade() );
        assertEquals( "Sé", dto.getBairro() );
        assertEquals( "Praça da Sé", dto.getLogradouro() );
        assertEquals( "1", dto.getNumero() );
        assertEquals( "sala 2", dto.getComplemento() );
    }

    @Test
    void deveAtualizarCamposEditaveisPreservandoIdentidadeAuditoriaERelacoesGerenciadas() {
        var id = UUID.randomUUID();
        var pais = Pais.builder().id( UUID.randomUUID() ).nome( "Brasil" ).build();
        var estado = Estado.builder().id( UUID.randomUUID() ).pais( pais ).nome( "Paraná" ).siglaEstado( "PR" ).build();
        var criacao = LocalDateTime.of( 2020, 1, 1, 0, 0 );
        var exclusao = LocalDateTime.of( 2024, 1, 1, 0, 0 );
        var entidade = new Endereco();
        entidade.setId( id );
        entidade.setDataCriacao( criacao );
        entidade.setDataExclusao( exclusao );
        entidade.setPais( pais );
        entidade.setEstado( estado );
        var dto = new EnderecoDTO();
        dto.setId( UUID.randomUUID() );
        dto.setCep( "80010000" );
        dto.setCidade( "Curitiba" );
        dto.setBairro( "Centro" );
        dto.setLogradouro( "Rua A" );
        dto.setNumero( "10" );
        dto.setComplemento( "fundos" );
        mapper.atualizar( dto, entidade );
        assertEquals( id, entidade.getId() );
        assertEquals( criacao, entidade.getDataCriacao() );
        assertEquals( exclusao, entidade.getDataExclusao() );
        assertSame( pais, entidade.getPais() );
        assertSame( estado, entidade.getEstado() );
        assertEquals( dto.getCep(), entidade.getCep() );
        assertEquals( dto.getCidade(), entidade.getCidade() );
        assertEquals( dto.getBairro(), entidade.getBairro() );
        assertEquals( dto.getLogradouro(), entidade.getLogradouro() );
        assertEquals( dto.getNumero(), entidade.getNumero() );
        assertEquals( dto.getComplemento(), entidade.getComplemento() );
    }
}
