package br.com.gems.app.solicitacao.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.gems.app.model.entity.parceiro.Parceiro;
import br.com.gems.app.model.entity.solicitacao.EtapaSolicitacao;
import br.com.gems.app.model.entity.solicitacao.Solicitacao;
import br.com.gems.app.model.enums.SituacaoSolicitacao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SolicitacaoMapperTest {
    private final SolicitacaoMapper mapper = Mappers.getMapper( SolicitacaoMapper.class );

    @Test
    void deveMapearParceiroEEtapas_quandoSolicitacaoInformada() {
        var parceiro = new Parceiro();
        parceiro.setId( UUID.randomUUID() );
        parceiro.setNome( "Parceiro X" );
        var data = LocalDateTime.of( 2025, 6, 7, 8, 9 );
        var etapa = new EtapaSolicitacao();
        etapa.setId( UUID.randomUUID() );
        etapa.setNome( "Documentos" );
        etapa.setOrdem( 3 );
        etapa.setConcluida( true );
        etapa.setDataConclusao( data );
        var solicitacao = new Solicitacao();
        solicitacao.setId( UUID.randomUUID() );
        solicitacao.setTitulo( "Pedido" );
        solicitacao.setDescricao( "Detalhes" );
        solicitacao.setSituacao( SituacaoSolicitacao.EM_ANDAMENTO );
        solicitacao.setMotivoCancelamento( "motivo histórico" );
        solicitacao.setDataConclusao( data );
        solicitacao.setParceiro( parceiro );
        solicitacao.setEtapas( List.of( etapa ) );
        var dto = mapper.toDTO( solicitacao );
        assertEquals( solicitacao.getId(), dto.getId() );
        assertEquals( "Pedido", dto.getTitulo() );
        assertEquals( "Detalhes", dto.getDescricao() );
        assertEquals( parceiro.getId(), dto.getParceiroId() );
        assertEquals( "Parceiro X", dto.getParceiroNome() );
        assertEquals( SituacaoSolicitacao.EM_ANDAMENTO, dto.getSituacao() );
        assertEquals( "motivo histórico", dto.getMotivoCancelamento() );
        assertEquals( data, dto.getDataConclusao() );
        assertEquals( 1, dto.getEtapas().size() );
        var etapaDto = dto.getEtapas().getFirst();
        assertEquals( etapa.getId(), etapaDto.getId() );
        assertEquals( "Documentos", etapaDto.getNome() );
        assertEquals( 3, etapaDto.getOrdem() );
        assertEquals( true, etapaDto.getConcluida() );
        assertEquals( data, etapaDto.getDataConclusao() );
    }

    @Test
    void deveMapearCampos_quandoEtapaInformada() {
        var data = LocalDateTime.of( 2025, 6, 7, 8, 9 );
        var etapa = new EtapaSolicitacao();
        etapa.setId( UUID.randomUUID() );
        etapa.setNome( "Análise" );
        etapa.setOrdem( 1 );
        etapa.setConcluida( false );
        etapa.setDataConclusao( data );
        var dto = mapper.toDTO( etapa );
        assertEquals( etapa.getId(), dto.getId() );
        assertEquals( "Análise", dto.getNome() );
        assertEquals( 1, dto.getOrdem() );
        assertEquals( false, dto.getConcluida() );
        assertEquals( data, dto.getDataConclusao() );
    }
}
