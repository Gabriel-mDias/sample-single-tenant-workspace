package br.com.gems.app.localidade.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.com.gems.app.model.entity.localidades.Estado;
import br.com.gems.app.model.entity.localidades.Pais;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class LocalidadeMapperTest {
    private final LocalidadeMapper mapper = Mappers.getMapper( LocalidadeMapper.class );

    @Test
    void deveMapearCampos_quandoPaisInformado() {
        var pais = Pais.builder().id( UUID.randomUUID() ).nome( "Brasil" ).build();
        var dto = mapper.toPaisDTO( pais );
        assertEquals( pais.getId(), dto.getId() );
        assertEquals( "Brasil", dto.getNome() );
    }

    @Test
    void deveIgnorarPais_quandoMapearEstadoParaListagem() {
        var pais = Pais.builder().id( UUID.randomUUID() ).nome( "Brasil" ).build();
        var estado = Estado.builder().id( UUID.randomUUID() ).pais( pais ).nome( "São Paulo" ).siglaEstado( "SP" )
                .build();
        var dto = mapper.toEstadoDTO( estado );
        assertEquals( estado.getId(), dto.getId() );
        assertEquals( "São Paulo", dto.getNome() );
        assertEquals( "SP", dto.getSiglaEstado() );
        assertNull( dto.getPais() );
    }
}
