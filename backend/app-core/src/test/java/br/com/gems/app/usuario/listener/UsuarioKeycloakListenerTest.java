package br.com.gems.app.usuario.listener;

import br.com.gems.app.model.entity.seguranca.Usuario;
import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.security.dto.CreateUserDTO;
import br.com.gems.app.security.service.KeycloakGroupService;
import br.com.gems.app.security.service.KeycloakUserService;
import br.com.gems.app.usuario.event.UsuarioAtualizadoEvent;
import br.com.gems.app.usuario.event.UsuarioCriadoEvent;
import br.com.gems.app.usuario.event.UsuarioDesativadoEvent;
import br.com.gems.app.usuario.event.UsuarioReativadoEvent;
import br.com.gems.exception.exception.SecurityException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cobertura da sincronização com o Keycloak e, sobretudo, da <b>compensação transacional</b> — a
 * garantia de que não sobram usuários órfãos no realm nem usuárioes sem acesso no banco.
 *
 * <p>
 * Não sobe contexto Spring: {@code TransactionSynchronizationManager} é estático e perfeitamente
 * pilotável à mão. {@code initSynchronization()} no {@code @BeforeEach} é o que faz
 * {@code isSynchronizationActive()} responder {@code true} sem transação real, e disparar
 * {@code afterCompletion} manualmente é o que simula commit e rollback.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioKeycloakListenerTest {

    private static final String KEYCLOAK_ID = "kc-0001";
    private static final String GRUPO_ID_OPERADOR = "grp-operador";
    private static final String GRUPO_ID_CONSULTA = "grp-consulta";

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private KeycloakGroupService keycloakGroupService;

    @InjectMocks
    private UsuarioKeycloakListener listener;

    @BeforeEach
    void abrirSincronizacao() {
        TransactionSynchronizationManager.initSynchronization();

        when( keycloakUserService.createUser( any( CreateUserDTO.class ) ) ).thenReturn( KEYCLOAK_ID );
        when( keycloakUserService.getGrupos( KEYCLOAK_ID ) ).thenReturn( List.of() );
        when( keycloakGroupService.buscarGrupo( "OPERADOR" ) )
                .thenReturn( Optional.of( grupo( GRUPO_ID_OPERADOR, "OPERADOR" ) ) );
        when( keycloakGroupService.buscarGrupo( "CONSULTA" ) )
                .thenReturn( Optional.of( grupo( GRUPO_ID_CONSULTA, "CONSULTA" ) ) );
    }

    @AfterEach
    void fecharSincronizacao() {
        if ( TransactionSynchronizationManager.isSynchronizationActive() ) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("criação: o id do Keycloak é escrito na instância que ainda será persistida")
    void deveComprovarCriacaoGravaKeycloakIdNaInstancia_quandoExecutaOCenario() {
        var usuario = usuarioTransiente();

        listener.aoCriar( eventoCriacao( usuario, PerfilAtuacao.OPERADOR ) );

        // É isto que permite CD_KEYCLOAK_ID ser NOT NULL: quando o service salvar, o campo já está lá.
        assertThat( usuario.getKeycloakId() ).isEqualTo( KEYCLOAK_ID );
    }

    @Test
    @DisplayName("criação: o usuário entra no grupo do seu perfil, e nenhuma role é atribuída a ele")
    void deveComprovarCriacaoColocaNoGrupoDoPerfil_quandoExecutaOCenario() {
        listener.aoCriar( eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) );

        verify( keycloakUserService ).entrarNoGrupo( KEYCLOAK_ID, GRUPO_ID_OPERADOR );
    }

    @Test
    @DisplayName("criação: rollback da transação apaga a conta criada no Keycloak")
    void deveComprovarCriacaoCompensaNoRollback_quandoExecutaOCenario() {
        listener.aoCriar( eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) );

        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );

        verify( keycloakUserService ).deleteUser( KEYCLOAK_ID );
    }

    @Test
    @DisplayName("criação: commit não dispara compensação nenhuma")
    void deveComprovarCriacaoNaoCompensaNoCommit_quandoExecutaOCenario() {
        listener.aoCriar( eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) );

        dispararDesfecho( TransactionSynchronization.STATUS_COMMITTED );

        verify( keycloakUserService, never() ).deleteUser( anyString() );
    }

    @Test
    @DisplayName("criação: falha do Keycloak propaga, para que a transação inteira caia")
    void deveComprovarFalhaDoKeycloakPropaga_quandoExecutaOCenario() {
        when( keycloakUserService.createUser( any( CreateUserDTO.class ) ) )
                .thenThrow( new SecurityException( "Falha ao criar o usuário junto a API de Autenticação." ) );

        var evento = eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR );

        assertThatThrownBy( () -> listener.aoCriar( evento ) )
                .isInstanceOf( SecurityException.class );

        verify( keycloakUserService, never() ).entrarNoGrupo( anyString(), anyString() );
    }

    @Test
    @DisplayName("criação: grupo inexistente no realm é erro explícito, não filiação silenciosa")
    void deveComprovarGrupoInexistenteFalhaExplicitamente_quandoExecutaOCenario() {
        when( keycloakGroupService.buscarGrupo( "OPERADOR" ) ).thenReturn( Optional.empty() );

        var evento = eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR );

        assertThatThrownBy( () -> listener.aoCriar( evento ) )
                .isInstanceOf( SecurityException.class )
                .hasMessageContaining( "não existe no realm" );
    }

    @Test
    @DisplayName("atualização: trocar de perfil move o usuário de grupo")
    void deveComprovarAtualizacaoTrocaDeGrupo_quandoExecutaOCenario() {
        when( keycloakUserService.getUser( KEYCLOAK_ID ) ).thenReturn( representacao() );
        when( keycloakUserService.getGrupos( KEYCLOAK_ID ) ).thenReturn( List.of( "OPERADOR" ) );

        listener.aoAtualizar( new UsuarioAtualizadoEvent(
                usuarioPersistido(), PerfilAtuacao.CONSULTA, "ana@exemplo.com.br", "Ana", "Souza" ) );

        verify( keycloakUserService ).sairDoGrupo( KEYCLOAK_ID, GRUPO_ID_OPERADOR );
        verify( keycloakUserService ).entrarNoGrupo( KEYCLOAK_ID, GRUPO_ID_CONSULTA );
    }

    @Test
    @DisplayName("atualização: os dados cadastrais são propagados ao Keycloak")
    void deveComprovarAtualizacaoPropagaDadosCadastrais_quandoExecutaOCenario() {
        when( keycloakUserService.getUser( KEYCLOAK_ID ) ).thenReturn( representacao() );

        listener.aoAtualizar( new UsuarioAtualizadoEvent(
                usuarioPersistido(), PerfilAtuacao.CONSULTA, "novo@exemplo.com.br", "Ana Paula", "Souza Lima" ) );

        var captor = ArgumentCaptor.forClass( UserRepresentation.class );
        verify( keycloakUserService ).updateUser( eq( KEYCLOAK_ID ), captor.capture() );

        var enviado = captor.getValue();
        assertThat( enviado.getEmail() ).isEqualTo( "novo@exemplo.com.br" );
        assertThat( enviado.getFirstName() ).isEqualTo( "Ana Paula" );
        assertThat( enviado.getLastName() ).isEqualTo( "Souza Lima" );
        // O login é a identidade da conta e não muda por este caminho.
        assertThat( enviado.getUsername() ).isEqualTo( "ana.souza" );
    }

    @Test
    @DisplayName("atualização: rollback restaura o snapshot anterior e devolve o grupo de origem")
    void deveComprovarAtualizacaoCompensaNoRollback_quandoExecutaOCenario() {
        var anterior = representacao();
        when( keycloakUserService.getUser( KEYCLOAK_ID ) ).thenReturn( anterior );
        when( keycloakUserService.getGrupos( KEYCLOAK_ID ) )
                .thenReturn( List.of( "OPERADOR" ) ) // snapshot, antes da troca
                .thenReturn( List.of( "OPERADOR" ) ) // leitura interna da troca de grupo
                .thenReturn( List.of( "CONSULTA" ) ); // estado no momento da compensação

        listener.aoAtualizar( new UsuarioAtualizadoEvent(
                usuarioPersistido(), PerfilAtuacao.CONSULTA, "ana@exemplo.com.br", "Ana", "Souza" ) );

        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );

        verify( keycloakUserService ).updateUser( KEYCLOAK_ID, anterior );
        verify( keycloakUserService ).sairDoGrupo( KEYCLOAK_ID, GRUPO_ID_CONSULTA );
        verify( keycloakUserService ).entrarNoGrupo( KEYCLOAK_ID, GRUPO_ID_OPERADOR );
    }

    @Test
    @DisplayName("encerramento: a conta é desabilitada, nunca excluída")
    void deveComprovarDesativacaoDesabilitaSemExcluir_quandoExecutaOCenario() {
        listener.aoDesativar( new UsuarioDesativadoEvent( usuarioPersistido() ) );

        verify( keycloakUserService ).disableUser( KEYCLOAK_ID );
        verify( keycloakUserService, never() ).deleteUser( anyString() );
    }

    @Test
    @DisplayName("encerramento: rollback reabilita a conta")
    void deveComprovarDesativacaoCompensaNoRollback_quandoExecutaOCenario() {
        listener.aoDesativar( new UsuarioDesativadoEvent( usuarioPersistido() ) );

        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );

        verify( keycloakUserService ).setUserEnabled( KEYCLOAK_ID, true );
    }

    @Test
    @DisplayName("retorno à equipe: a conta é reabilitada e recolocada no grupo, sem senha nova")
    void deveComprovarReativacaoReabilitaSemSenhaNova_quandoExecutaOCenario() {
        listener.aoReativar( new UsuarioReativadoEvent( usuarioPersistido(), PerfilAtuacao.CONSULTA ) );

        verify( keycloakUserService ).setUserEnabled( KEYCLOAK_ID, true );
        verify( keycloakUserService ).entrarNoGrupo( KEYCLOAK_ID, GRUPO_ID_CONSULTA );
        verify( keycloakUserService, never() ).createUser( any( CreateUserDTO.class ) );
    }

    @Test
    @DisplayName("retorno à equipe: rollback volta a desabilitar a conta")
    void deveComprovarReativacaoCompensaNoRollback_quandoExecutaOCenario() {
        listener.aoReativar( new UsuarioReativadoEvent( usuarioPersistido(), PerfilAtuacao.CONSULTA ) );

        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );

        verify( keycloakUserService ).setUserEnabled( KEYCLOAK_ID, false );
    }

    @Test
    @DisplayName("falha DENTRO da compensação é registrada e engolida — afterCompletion nunca lança")
    void deveComprovarFalhaNaCompensacaoNaoPropaga_quandoExecutaOCenario() {
        doThrow( new IllegalStateException( "Keycloak fora do ar" ) )
                .when( keycloakUserService ).deleteUser( KEYCLOAK_ID );

        listener.aoCriar( eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) );

        // Se isto lançasse, o gerenciador de transação mascararia a causa original da falha.
        dispararDesfecho( TransactionSynchronization.STATUS_ROLLED_BACK );
    }

    @Test
    @DisplayName("sem transação ativa não há compensação a registrar, e nada quebra")
    void deveComprovarSemTransacaoAtivaNaoRegistraCompensacao_quandoExecutaOCenario() {
        TransactionSynchronizationManager.clearSynchronization();

        listener.aoCriar( eventoCriacao( usuarioTransiente(), PerfilAtuacao.OPERADOR ) );

        verify( keycloakUserService ).entrarNoGrupo( KEYCLOAK_ID, GRUPO_ID_OPERADOR );
    }

    private void dispararDesfecho( int status ) {
        List.copyOf( TransactionSynchronizationManager.getSynchronizations() )
                .forEach( sincronizacao -> sincronizacao.afterCompletion( status ) );
    }

    private UsuarioCriadoEvent eventoCriacao( Usuario usuario, PerfilAtuacao perfil ) {
        return new UsuarioCriadoEvent( usuario, "Senha@Forte123", perfil, "ana@exemplo.com.br", "Ana", "Souza" );
    }

    private Usuario usuarioTransiente() {
        return Usuario.builder().login( "ana.souza" ).ativo( true ).build();
    }

    private Usuario usuarioPersistido() {
        return Usuario.builder()
                .id( UUID.fromString( "77777777-7777-7777-7777-777777777777" ) )
                .login( "ana.souza" )
                .keycloakId( KEYCLOAK_ID )
                .ativo( true )
                .build();
    }

    private UserRepresentation representacao() {
        var representacao = new UserRepresentation();
        representacao.setId( KEYCLOAK_ID );
        representacao.setUsername( "ana.souza" );
        representacao.setEmail( "ana@exemplo.com.br" );
        representacao.setFirstName( "Ana" );
        representacao.setLastName( "Souza" );
        representacao.setEnabled( true );
        return representacao;
    }

    private GroupRepresentation grupo( String id, String nome ) {
        var grupo = new GroupRepresentation();
        grupo.setId( id );
        grupo.setName( nome );
        return grupo;
    }

}
