package br.com.gems.app.parceiro.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.model.entity.normalizacao.Endereco;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.entity.localidades.Pais;
import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.enums.Sexo;
import br.com.gems.app.normalizacao.mapper.ContatoMapper;
import br.com.gems.app.normalizacao.mapper.EnderecoMapper;
import br.com.gems.app.normalizacao.mapper.PessoaMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

class ParceiroMapperTest {
    private final ParceiroMapper mapper = Mappers.getMapper( ParceiroMapper.class );

    ParceiroMapperTest() {
        ReflectionTestUtils.setField( mapper, "pessoaMapper", Mappers.getMapper( PessoaMapper.class ) );
        ReflectionTestUtils.setField( mapper, "contatoMapper", Mappers.getMapper( ContatoMapper.class ) );
        ReflectionTestUtils.setField( mapper, "enderecoMapper", Mappers.getMapper( EnderecoMapper.class ) );
    }

    @Test
    void deveMapearTodosOsCamposDoParceiroEAgregados() {
        var p = new Pessoa();
        p.setId( UUID.randomUUID() );
        p.setPrimeiroNome( "Ana" );
        p.setSobrenome( "Lima" );
        p.setDataNascimento( LocalDate.of( 1990, 2, 3 ) );
        p.setCpf( "12345678901" );
        p.setRg( "RG1" );
        p.setSexo( Sexo.FEMININO );
        p.setDataObito( null );
        p.setDataAproximada( false );
        var c = new Contato();
        c.setId( UUID.randomUUID() );
        c.setEmail( "a@example.test" );
        c.setTelefone( "11999" );
        var pais = Pais.builder().id( UUID.randomUUID() ).nome( "Brasil" ).build();
        var estado = Estado.builder().id( UUID.randomUUID() ).pais( pais ).nome( "São Paulo" ).siglaEstado( "SP" )
                .build();
        var e = new Endereco();
        e.setId( UUID.randomUUID() );
        e.setPais( pais );
        e.setEstado( estado );
        e.setCep( "01001000" );
        e.setCidade( "São Paulo" );
        e.setBairro( "Sé" );
        e.setLogradouro( "Rua 1" );
        e.setNumero( "2" );
        e.setComplemento( "apto" );
        var entity = new Parceiro();
        entity.setId( UUID.randomUUID() );
        entity.setNome( "Parceiro A" );
        entity.setDescricao( "descrição" );
        entity.setComissao( new BigDecimal( "12.50" ) );
        entity.setSolicitacoesConcluidas( 7 );
        entity.setPessoa( p );
        entity.setContato( c );
        entity.setEndereco( e );
        var dto = mapper.toDTO( entity );
        assertEquals( entity.getId(), dto.getId() );
        assertEquals( "Parceiro A", dto.getNome() );
        assertEquals( "descrição", dto.getDescricao() );
        assertEquals( new BigDecimal( "12.50" ), dto.getComissao() );
        assertEquals( 7, dto.getSolicitacoesConcluidas() );
        assertEquals( p.getId(), dto.getPessoa().getId() );
        assertEquals( "Ana", dto.getPessoa().getPrimeiroNome() );
        assertEquals( "Lima", dto.getPessoa().getSobrenome() );
        assertEquals( p.getDataNascimento(), dto.getPessoa().getDataNascimento() );
        assertEquals( p.getCpf(), dto.getPessoa().getCpf() );
        assertEquals( p.getRg(), dto.getPessoa().getRg() );
        assertEquals( p.getSexo(), dto.getPessoa().getSexo() );
        assertEquals( p.getDataObito(), dto.getPessoa().getDataObito() );
        assertEquals( p.getDataAproximada(), dto.getPessoa().getDataAproximada() );
        assertEquals( c.getId(), dto.getContato().getId() );
        assertEquals( c.getEmail(), dto.getContato().getEmail() );
        assertEquals( c.getTelefone(), dto.getContato().getTelefone() );
        assertEquals( e.getId(), dto.getEndereco().getId() );
        assertEquals( pais.getId(), dto.getEndereco().getPais().getId() );
        assertEquals( estado.getId(), dto.getEndereco().getEstado().getId() );
        assertEquals( e.getCep(), dto.getEndereco().getCep() );
        assertEquals( e.getCidade(), dto.getEndereco().getCidade() );
        assertEquals( e.getBairro(), dto.getEndereco().getBairro() );
        assertEquals( e.getLogradouro(), dto.getEndereco().getLogradouro() );
        assertEquals( e.getNumero(), dto.getEndereco().getNumero() );
        assertEquals( e.getComplemento(), dto.getEndereco().getComplemento() );
    }
}
