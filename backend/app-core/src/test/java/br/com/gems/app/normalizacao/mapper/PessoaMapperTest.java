package br.com.gems.app.normalizacao.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.com.gems.app.model.dto.PessoaDTO;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.enums.Sexo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class PessoaMapperTest {
    private final PessoaMapper mapper = Mappers.getMapper( PessoaMapper.class );

    @Test
    void deveMapearTodosOsCamposPublicosParaDTO() {
        var id = UUID.randomUUID();
        var nascimento = LocalDate.of( 1985, 4, 12 );
        var obito = LocalDate.of( 2020, 3, 1 );
        var pessoa = new Pessoa();
        pessoa.setId( id );
        pessoa.setPrimeiroNome( "Ana" );
        pessoa.setSobrenome( "Silva" );
        pessoa.setDataNascimento( nascimento );
        pessoa.setCpf( "12345678901" );
        pessoa.setRg( "RG-42" );
        pessoa.setSexo( Sexo.FEMININO );
        pessoa.setDataObito( obito );
        pessoa.setDataAproximada( true );
        pessoa.setDataCriacao( LocalDateTime.of( 2020, 1, 1, 0, 0 ) );
        pessoa.setDataExclusao( LocalDateTime.of( 2024, 1, 1, 0, 0 ) );

        var dto = mapper.toDTO( pessoa );

        assertEquals( id, dto.getId() );
        assertEquals( "Ana", dto.getPrimeiroNome() );
        assertEquals( "Silva", dto.getSobrenome() );
        assertEquals( nascimento, dto.getDataNascimento() );
        assertEquals( "12345678901", dto.getCpf() );
        assertEquals( "RG-42", dto.getRg() );
        assertEquals( Sexo.FEMININO, dto.getSexo() );
        assertEquals( obito, dto.getDataObito() );
        assertEquals( true, dto.getDataAproximada() );
    }

    @Test
    void deveAtualizarCamposEditaveisEPreservarIdentidadeEAuditoria() {
        var id = UUID.randomUUID();
        var criacao = LocalDateTime.of( 2020, 1, 1, 0, 0 );
        var exclusao = LocalDateTime.of( 2025, 1, 1, 0, 0 );
        var entidade = new Pessoa();
        entidade.setId( id );
        entidade.setDataCriacao( criacao );
        entidade.setDataExclusao( exclusao );
        var dto = new PessoaDTO();
        dto.setId( UUID.randomUUID() );
        dto.setPrimeiroNome( "Bia" );
        dto.setSobrenome( "Souza" );
        dto.setDataNascimento( LocalDate.of( 1990, 2, 3 ) );
        dto.setCpf( "98765432100" );
        dto.setRg( "RG-9" );
        dto.setSexo( Sexo.FEMININO );
        dto.setDataObito( LocalDate.of( 2024, 1, 2 ) );
        dto.setDataAproximada( false );

        mapper.atualizar( dto, entidade );

        assertEquals( id, entidade.getId() );
        assertEquals( criacao, entidade.getDataCriacao() );
        assertEquals( exclusao, entidade.getDataExclusao() );
        assertEquals( "Bia", entidade.getPrimeiroNome() );
        assertEquals( "Souza", entidade.getSobrenome() );
        assertEquals( dto.getDataNascimento(), entidade.getDataNascimento() );
        assertEquals( dto.getCpf(), entidade.getCpf() );
        assertEquals( dto.getRg(), entidade.getRg() );
        assertEquals( dto.getSexo(), entidade.getSexo() );
        assertEquals( dto.getDataObito(), entidade.getDataObito() );
        assertEquals( dto.getDataAproximada(), entidade.getDataAproximada() );
    }
}
