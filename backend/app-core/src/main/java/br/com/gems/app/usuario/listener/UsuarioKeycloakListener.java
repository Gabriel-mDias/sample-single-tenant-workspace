package br.com.gems.app.usuario.listener;

import br.com.gems.app.model.enums.PerfilAtuacao;
import br.com.gems.app.security.dto.CreateUserDTO;
import br.com.gems.app.security.service.KeycloakGroupService;
import br.com.gems.app.security.service.KeycloakUserService;
import br.com.gems.app.usuario.event.UsuarioAtualizadoEvent;
import br.com.gems.app.usuario.event.UsuarioCriadoEvent;
import br.com.gems.app.usuario.event.UsuarioDesativadoEvent;
import br.com.gems.app.usuario.event.UsuarioReativadoEvent;
import br.com.gems.exception.exception.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Espelha no Keycloak o ciclo de vida das credenciais do sistema.
 *
 * <h2>Por que aqui, e não em {@code app-events}</h2>
 * Este listener precisa do {@link KeycloakUserService}, que vive em {@code app-security}, e
 * {@code app-events} depende apenas de {@code app-model}. Mover a dependência alteraria o grafo
 * do reator e exigiria emenda constitucional por um detalhe de empacotamento — os POJOs de evento
 * ficam lá, o listener nasce aqui.
 *
 * <h2>Por que {@code @EventListener} e não {@code @TransactionalEventListener}</h2>
 * Um listener {@code AFTER_COMMIT} rodaria com o cadastro <b>já confirmado</b>: uma falha do
 * Keycloak deixaria um usuário sem acesso, e como os eventos deste projeto são entregues apenas
 * em memória, uma queda do processo perderia o evento para sempre. Aqui o listener roda dentro da
 * transação: se o Keycloak recusar, a exceção sobe e nada é gravado.
 *
 * <h2>Compensação</h2>
 * O inverso também precisa valer: se a transação cair <b>depois</b> de o Keycloak já ter respondido,
 * o que foi feito lá tem de ser desfeito. Cada operação registra sua própria compensação, executada
 * em {@code afterCompletion} apenas quando a transação termina em rollback.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioKeycloakListener {

    private final KeycloakUserService keycloakUserService;
    private final KeycloakGroupService keycloakGroupService;

    /**
     * Provisiona a conta e a coloca no grupo do perfil.
     *
     * <p>
     * O identificador devolvido pelo Keycloak é gravado na instância ainda transiente do evento —
     * quem persiste é o {@code UsuarioService}, logo depois. Assim a coluna {@code CD_KEYCLOAK_ID}
     * nunca precisa aceitar nulo.
     */
    @EventListener
    public void aoCriar( UsuarioCriadoEvent evento ) {
        var keycloakId = keycloakUserService.createUser( CreateUserDTO.builder()
                .username( evento.usuario().getLogin() )
                .email( evento.email() )
                .firstName( evento.primeiroNome() )
                .lastName( evento.sobrenome() )
                .password( evento.senha() )
                .build() );

        // Registrada imediatamente após a criação: daqui em diante, qualquer falha — inclusive a
        // filiação ao grupo, logo abaixo — deixa uma conta órfã se não for desfeita.
        registrarCompensacao( () -> keycloakUserService.deleteUser( keycloakId ) );

        entrarNoGrupoDoPerfil( keycloakId, evento.perfil() );

        evento.usuario().setKeycloakId( keycloakId );
    }

    /**
     * Sincroniza dados cadastrais e filiação a grupo.
     *
     * <p>
     * O snapshot é lido <b>antes</b> de qualquer alteração: é ele que a compensação usa para
     * devolver a conta ao estado anterior.
     */
    @EventListener
    public void aoAtualizar( UsuarioAtualizadoEvent evento ) {
        var keycloakId = evento.usuario().getKeycloakId();

        var anterior = keycloakUserService.getUser( keycloakId );
        var gruposAnteriores = keycloakUserService.getGrupos( keycloakId );

        registrarCompensacao( () -> {
            keycloakUserService.updateUser( keycloakId, anterior );
            restaurarGrupos( keycloakId, gruposAnteriores );
        } );

        var atual = new UserRepresentation();
        atual.setId( anterior.getId() );
        atual.setUsername( anterior.getUsername() );
        atual.setEnabled( anterior.isEnabled() );
        atual.setEmail( evento.email() );
        atual.setFirstName( evento.primeiroNome() );
        atual.setLastName( evento.sobrenome() );

        keycloakUserService.updateUser( keycloakId, atual );
        entrarNoGrupoDoPerfil( keycloakId, evento.perfil() );
    }

    /**
     * Reabilita a conta de quem retorna à equipe e a recoloca no grupo do novo perfil.
     */
    @EventListener
    public void aoReativar( UsuarioReativadoEvent evento ) {
        var keycloakId = evento.usuario().getKeycloakId();
        var gruposAnteriores = keycloakUserService.getGrupos( keycloakId );

        registrarCompensacao( () -> {
            keycloakUserService.setUserEnabled( keycloakId, false );
            restaurarGrupos( keycloakId, gruposAnteriores );
        } );

        keycloakUserService.setUserEnabled( keycloakId, true );
        entrarNoGrupoDoPerfil( keycloakId, evento.perfil() );
    }

    /**
     * Desabilita a conta ao encerrar o vínculo. Nada é excluído — nem aqui, nem no Keycloak.
     */
    @EventListener
    public void aoDesativar( UsuarioDesativadoEvent evento ) {
        var keycloakId = evento.usuario().getKeycloakId();

        registrarCompensacao( () -> keycloakUserService.setUserEnabled( keycloakId, true ) );

        keycloakUserService.disableUser( keycloakId );
    }

    /**
     * Move o usuário para o grupo do perfil informado, saindo de qualquer outro grupo de perfil.
     *
     * <p>
     * Um usuário pertence a exatamente um grupo de perfil por vez — é isso que torna a
     * permissão efetiva previsível.
     */
    private void entrarNoGrupoDoPerfil( String keycloakId, PerfilAtuacao perfil ) {
        var destino = keycloakGroupService.buscarGrupo( perfil.getNomeGrupo() )
                .orElseThrow( () -> new SecurityException(
                        "O grupo '" + perfil.getNomeGrupo() + "' não existe no realm. "
                                + "Verifique o bootstrap de grupos e as permissões do service account." ) );

        var gruposDeOutrosPerfis = List.of( PerfilAtuacao.values() )
                .stream()
                .filter( outro -> outro != perfil )
                .map( PerfilAtuacao::getNomeGrupo )
                .toList();

        keycloakUserService.getGrupos( keycloakId )
                .stream()
                .filter( gruposDeOutrosPerfis::contains )
                .forEach( nome -> keycloakGroupService.buscarGrupo( nome )
                        .ifPresent( grupo -> keycloakUserService.sairDoGrupo( keycloakId, grupo.getId() ) ) );

        keycloakUserService.entrarNoGrupo( keycloakId, destino.getId() );
    }

    private void restaurarGrupos( String keycloakId, List<String> gruposAnteriores ) {
        var atuais = keycloakUserService.getGrupos( keycloakId );

        atuais.stream()
                .filter( nome -> !gruposAnteriores.contains( nome ) )
                .forEach( nome -> keycloakGroupService.buscarGrupo( nome )
                        .ifPresent( grupo -> keycloakUserService.sairDoGrupo( keycloakId, grupo.getId() ) ) );

        gruposAnteriores.stream()
                .filter( nome -> !atuais.contains( nome ) )
                .forEach( nome -> keycloakGroupService.buscarGrupo( nome )
                        .ifPresent( grupo -> keycloakUserService.entrarNoGrupo( keycloakId, grupo.getId() ) ) );
    }

    /**
     * Agenda o desfazimento para o caso de a transação terminar em rollback.
     *
     * <p>
     * {@code afterCompletion} é o único gancho que enxerga o desfecho real — ele roda depois do
     * commit ou do rollback físico. E ele <b>nunca pode lançar</b>: uma exceção ali seria engolida
     * pelo gerenciador de transação e mascararia a causa original da falha. Um órfão registrado em
     * log é um problema muito menor do que um erro invisível.
     *
     * <p>
     * Fora de transação não há o que compensar — é o caso dos testes que exercitam o listener
     * isoladamente sem abrir sincronização.
     */
    private void registrarCompensacao( Runnable desfazer ) {
        if ( !TransactionSynchronizationManager.isSynchronizationActive() ) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization( new TransactionSynchronization() {
            @Override
            public void afterCompletion( int status ) {
                if ( status != TransactionSynchronization.STATUS_ROLLED_BACK ) {
                    return;
                }

                try {
                    desfazer.run();
                } catch ( Exception e ) {
                    log.error( "Compensação no Keycloak falhou após rollback. "
                            + "Verifique manualmente se restou usuário órfão ou filiação indevida no realm.", e );
                }
            }
        } );
    }

}
