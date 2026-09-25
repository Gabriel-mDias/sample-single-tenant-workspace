package br.com.gems.app.commons.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseDTOTest {

    @Test
    void deveSerializarSomenteContratoEstavel_quandoPaginaNaoTrivial() throws Exception {
        // Arrange
        var pagina = new PageImpl<>( List.of( "ultimo item" ), PageRequest.of( 2, 10 ), 21 );
        var resposta = PageResponseDTO.from( pagina );

        // Act
        var mapper = new ObjectMapper();
        var json = mapper.readTree( mapper.writeValueAsBytes( resposta ) );

        // Assert
        assertThat( json.size() ).isEqualTo( 5 );
        assertThat( json.get( "content" ).get( 0 ).asText() ).isEqualTo( "ultimo item" );
        assertThat( json.get( "totalElements" ).asLong() ).isEqualTo( 21 );
        assertThat( json.get( "totalPages" ).asInt() ).isEqualTo( 3 );
        assertThat( json.get( "number" ).asInt() ).isEqualTo( 2 );
        assertThat( json.get( "size" ).asInt() ).isEqualTo( 10 );
    }

}
