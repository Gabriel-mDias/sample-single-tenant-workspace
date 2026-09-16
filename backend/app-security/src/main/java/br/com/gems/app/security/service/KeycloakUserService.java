package br.com.gems.app.security.service;

import br.com.gems.app.security.config.AppKeycloakProperties;
import br.com.gems.app.security.dto.CreateUserDTO;
import br.com.gems.exception.exception.BusinessException;
import br.com.gems.exception.exception.ExternalServiceException;
import br.com.gems.utils.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final Keycloak keycloak;
    private final AppKeycloakProperties keycloakProperties;

    private static final List<Integer> EXPECTED_SUCCESS_STATUS_FOR_CREATED_USER = List.of( 200, 201, 204 );

    private static final Integer CONFLICT_STATUS = 409;

    /**
     * Método responsável por criar um usuário no Keycloak.
     *
     * @param dto
     *            Dados mínimos para a criação de um usuário no Keycloak
     * @return O identificador único gerado para este usuário no Keycloak. Com este identificador, será possível
     *         desabilitá-lo.
     */
    public String createUser( CreateUserDTO dto ) {
        return executarNoKeycloak( "Falha ao criar o usuário junto a API de Autenticação.", () -> {
            if ( ObjectUtil.isNullOrEmpty( dto.password() ) ) {
                throw new BusinessException( "A senha não foi informada durante a criação de um novo usuário!" );
            }

            var user = buildUserRepresentation( dto );
            var usersResource = keycloak.realm( keycloakProperties.getRealm() ).users();
            String userId;

            try ( var response = usersResource.create( user ) ) {
                if ( CONFLICT_STATUS.equals( response.getStatus() ) ) {
                    throw new BusinessException( "Falha ao criar o usuário junto a API de Autenticação. "
                            + "Nome de usuário ou email já existente." );
                }

                if ( EXPECTED_SUCCESS_STATUS_FOR_CREATED_USER.stream()
                        .noneMatch( status -> status.equals( response.getStatus() ) ) ) {
                    var mensagem = "Falha ao criar o usuário junto a API de Autenticação. Status: "
                            + response.getStatus();
                    throw falhaNoKeycloak( mensagem, new IllegalStateException( mensagem ) );
                }

                userId = CreatedResponseUtil.getCreatedId( response );
            }

            try {
                setUsersPassword( userId, dto.password() );
            } catch ( RuntimeException exception ) {
                try {
                    deleteUser( userId );
                } catch ( RuntimeException compensationFailure ) {
                    exception.addSuppressed( compensationFailure );
                }
                throw falhaNoKeycloak(
                        "Falha na definição da senha do usuário junto a API de Autenticação!", exception );
            }

            return userId;
        } );
    }

    /**
     * Método responsável por desabilitar um usuário no Keycloak (impede novos logins).
     *
     * @param keycloakUserId
     *            identificador único do usuário no Keycloak.
     */
    public void disableUser( String keycloakUserId ) {
        setUserEnabled( keycloakUserId, false );
    }

    /**
     * Habilita ou desabilita um usuário no Keycloak. Desabilitar impede novos logins; habilitar restaura o
     * acesso.
     *
     * @param keycloakUserId
     *            identificador único do usuário no Keycloak.
     * @param enabled
     *            {@code true} para habilitar, {@code false} para desabilitar.
     */
    public void setUserEnabled( String keycloakUserId, boolean enabled ) {
        executarNoKeycloak( "Falha ao alterar a habilitação do usuário junto a API de Autenticação.", () -> {
            var usersResource = keycloak.realm( keycloakProperties.getRealm() ).users();
            var user = usersResource.get( keycloakUserId ).toRepresentation();

            user.setEnabled( enabled );
            usersResource.get( keycloakUserId ).update( user );
        } );
    }

    public void deleteUser( String keycloakUserId ) {
        executarNoKeycloak( "Falha ao excluir o usuário junto a API de Autenticação.", () -> {
            var usersResource = keycloak.realm( keycloakProperties.getRealm() ).users();
            usersResource.get( keycloakUserId ).remove();
        } );
    }

    /**
     * Lê a representação atual do usuário. Serve de <b>snapshot</b> antes de uma alteração: se a
     * transação cair depois de o Keycloak já ter sido atualizado, a compensação restaura este
     * estado.
     *
     * @param keycloakUserId
     *            identificador único do usuário no Keycloak.
     */
    public UserRepresentation getUser( String keycloakUserId ) {
        return executarNoKeycloak( "Falha ao consultar o usuário junto a API de Autenticação.",
                () -> keycloak.realm( keycloakProperties.getRealm() ).users().get( keycloakUserId )
                        .toRepresentation() );
    }

    /**
     * Atualiza os dados cadastrais do usuário (nome, sobrenome, e-mail).
     *
     * @param keycloakUserId
     *            identificador único do usuário no Keycloak.
     * @param representation
     *            representação já alterada — normalmente obtida por {@link #getUser}.
     */
    public void updateUser( String keycloakUserId, UserRepresentation representation ) {
        executarNoKeycloak( "Falha ao atualizar o usuário junto a API de Autenticação.",
                () -> keycloak.realm( keycloakProperties.getRealm() ).users().get( keycloakUserId )
                        .update( representation ) );
    }

    /**
     * Nomes dos grupos a que o usuário pertence.
     *
     * <p>
     * Usado pela compensação de uma troca de perfil: é preciso saber de onde o usuário saiu para
     * poder devolvê-lo caso a transação não seja confirmada.
     *
     * @param keycloakUserId
     *            identificador único do usuário no Keycloak.
     */
    public List<String> getGrupos( String keycloakUserId ) {
        return executarNoKeycloak( "Falha ao consultar os grupos do usuário junto a API de Autenticação.",
                () -> keycloak.realm( keycloakProperties.getRealm() ).users().get( keycloakUserId ).groups()
                        .stream()
                        .map( GroupRepresentation::getName )
                        .toList() );
    }

    /**
     * Coloca o usuário em um grupo. É por aqui — e <b>somente</b> por aqui — que um usuário
     * ganha permissão: as roles ADMIN/USER estão mapeadas nos grupos, não nos usuários, então o
     * sistema nunca atribui role diretamente a ninguém.
     *
     * @param keycloakUserId
     *            identificador único do usuário no Keycloak.
     * @param groupId
     *            identificador único do grupo no Keycloak.
     */
    public void entrarNoGrupo( String keycloakUserId, String groupId ) {
        executarNoKeycloak( "Falha ao incluir o usuário no grupo junto a API de Autenticação.",
                () -> keycloak.realm( keycloakProperties.getRealm() ).users().get( keycloakUserId )
                        .joinGroup( groupId ) );
    }

    /**
     * Remove o usuário de um grupo, revogando as permissões herdadas dele.
     *
     * @param keycloakUserId
     *            identificador único do usuário no Keycloak.
     * @param groupId
     *            identificador único do grupo no Keycloak.
     */
    public void sairDoGrupo( String keycloakUserId, String groupId ) {
        executarNoKeycloak( "Falha ao remover o usuário do grupo junto a API de Autenticação.",
                () -> keycloak.realm( keycloakProperties.getRealm() ).users().get( keycloakUserId )
                        .leaveGroup( groupId ) );
    }

    /**
     * Método que define a senha de um usuário pelo seu ID
     *
     * @param userId
     *            Identificador único do usuário no Keycloak
     * @param newPassword
     *            nova senha que será definida para o usuário
     */
    private void setUsersPassword( String userId, String newPassword ) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary( false );
        credential.setType( CredentialRepresentation.PASSWORD );
        credential.setValue( newPassword );

        keycloak.realm( keycloakProperties.getRealm() ).users().get( userId ).resetPassword( credential );
    }

    private UserRepresentation buildUserRepresentation( CreateUserDTO dto ) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled( true );
        user.setUsername( dto.username() );
        user.setEmail( dto.email() );
        user.setFirstName( dto.firstName() );
        user.setLastName( dto.lastName() );
        user.setAttributes( dto.attributes() );

        return user;
    }

    private <T> T executarNoKeycloak( String mensagem, Supplier<T> operacao ) {
        try {
            return operacao.get();
        } catch ( BusinessException | ExternalServiceException exception ) {
            throw exception;
        } catch ( RuntimeException exception ) {
            throw falhaNoKeycloak( mensagem, exception );
        }
    }

    private void executarNoKeycloak( String mensagem, Runnable operacao ) {
        executarNoKeycloak( mensagem, () -> {
            operacao.run();
            return null;
        } );
    }

    private ExternalServiceException falhaNoKeycloak( String mensagem, Throwable causa ) {
        return new ExternalServiceException( "Keycloak", mensagem, causa );
    }

}
