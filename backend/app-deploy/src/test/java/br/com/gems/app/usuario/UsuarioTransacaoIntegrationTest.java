package br.com.gems.app.usuario;

import br.com.gems.app.model.dto.UsuarioDTO;
import br.com.gems.app.model.entity.normalizacao.Contato;
import br.com.gems.app.model.entity.normalizacao.Pessoa;
import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.support.AbstractIntegrationTest;
import br.com.gems.app.usuario.listener.UsuarioKeycloakListener;
import br.com.gems.app.usuario.repository.UsuarioRepository;
import br.com.gems.app.usuario.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

/** Verifica as quatro entradas de escrita chamadas sem transação no teste. */
class UsuarioTransacaoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioService service;

    @MockitoBean
    private UsuarioRepository repository;

    @MockitoBean
    private UsuarioKeycloakListener listener;

    @Test
    void deveAbrirTransacao_quandoEscritasSaoChamadasExternamente() {
        var usuario = Usuario.builder().login( "ana.souza" ).ativo( true ).build();
        var dto = new UsuarioDTO();
        dto.setLogin( "ana.souza" );
        dto.setSenha( "Senha@Forte123" );
        var pessoa = Pessoa.builder().primeiroNome( "Ana" ).sobrenome( "Souza" ).build();
        var contato = Contato.builder().email( "ana@exemplo.com.br" ).build();
        doAnswer( chamada -> {
            assertThat( TransactionSynchronizationManager.isActualTransactionActive() ).isTrue();
            return chamada.getArgument( 0 );
        } ).when( repository ).save( any( Usuario.class ) );
        assertThat( AopUtils.isAopProxy( service ) ).isTrue();
        assertThat( TransactionSynchronizationManager.isActualTransactionActive() ).isFalse();

        service.insert( dto, PerfilAtuacao.OPERADOR, pessoa, contato );
        service.reativar( usuario, PerfilAtuacao.OPERADOR );
        service.sincronizar( usuario, PerfilAtuacao.OPERADOR, pessoa, contato );
        service.desativar( usuario );

        verify( repository, times( 4 ) ).save( any( Usuario.class ) );
        assertThat( TransactionSynchronizationManager.isActualTransactionActive() ).isFalse();
    }
}
