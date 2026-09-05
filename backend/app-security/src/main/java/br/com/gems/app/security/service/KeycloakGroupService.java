package br.com.gems.app.security.service;

import br.com.gems.app.security.config.AppKeycloakProperties;
import br.com.gems.exception.exception.SecurityException;
import br.com.gems.utils.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Operações de grupo no realm do Keycloak.
 *
 * <p>Grupos são o eixo de permissão do sistema: cada perfil de atuação é um grupo, e é o grupo que
 * carrega a realm role. Essa role é a <b>composta</b> do perfil
 * ({@code PERFIL_OPERADOR}, {@code PERFIL_CONSULTA}, …), que por sua vez contém as ações daquele
 * cargo. Nenhuma role é atribuída diretamente a usuário — o
 * o usuário apenas entra no grupo e herda o que ele carrega. Isso mantém a regra "perfil X concede
 * permissão Y" declarada no provedor de identidade, e não codificada em Java.
 *
 * <p>Este serviço é deliberadamente primitivo: ele não conhece {@code PerfilAtuacao} (que vive em
 * {@code app-model}, módulo do qual {@code app-security} não depende). Quem casa as duas metades
 * é o bootstrap, em {@code app-core}.
 */
@Service
@RequiredArgsConstructor
public class KeycloakGroupService {

    private static final List<Integer> STATUS_SUCESSO = List.of( 200, 201, 204 );
    private static final Integer STATUS_CONFLITO = 409;

    private final Keycloak keycloak;
    private final AppKeycloakProperties keycloakProperties;

    /**
     * Procura um grupo de primeiro nível pelo nome exato.
     *
     * @param nome nome do grupo no realm.
     * @return o grupo, ou {@code Optional.empty()} se não existir.
     */
    public Optional<GroupRepresentation> buscarGrupo( String nome ) {
        if ( ObjectUtil.isNullOrEmpty( nome ) ) {
            return Optional.empty();
        }

        // `groups(search)` faz busca parcial e pode trazer homônimos parciais; o filtro por igualdade
        // exata abaixo é o que garante que "OPERADOR" não case com "OPERADOR_SENIOR" no futuro.
        return keycloak.realm( keycloakProperties.getRealm() ).groups().groups( nome, 0, 100 )
                .stream()
                .filter( grupo -> nome.equals( grupo.getName() ) )
                .findFirst();
    }

    /**
     * Cria um grupo de primeiro nível.
     *
     * <p>Trata 409 como sucesso: duas instâncias subindo em paralelo tentariam criar o mesmo grupo,
     * e "já existe" é exatamente o estado desejado.
     *
     * @param nome nome do grupo a criar.
     * @return o identificador do grupo — recém-criado ou já existente.
     */
    public String criarGrupo( String nome ) {
        var grupo = new GroupRepresentation();
        grupo.setName( nome );

        try ( var response = keycloak.realm( keycloakProperties.getRealm() ).groups().add( grupo ) ) {
            if ( STATUS_CONFLITO.equals( response.getStatus() ) ) {
                return buscarGrupo( nome )
                        .map( GroupRepresentation::getId )
                        .orElseThrow( () -> new SecurityException(
                                "O grupo '" + nome + "' acusou conflito na criação mas não foi encontrado no realm." ) );
            }

            if ( STATUS_SUCESSO.stream().noneMatch( status -> status.equals( response.getStatus() ) ) ) {
                throw new SecurityException(
                        "Falha ao criar o grupo '" + nome + "' junto a API de Autenticação. Status: " + response.getStatus() );
            }

            return CreatedResponseUtil.getCreatedId( response );
        }
    }

    /**
     * Mapeia uma realm role no grupo, se ela ainda não estiver mapeada.
     *
     * <p><b>Não remove</b> mapeamentos que já existam: um administrador pode ter concedido
     * permissões extras a um grupo pelo console, e o bootstrap não pode desfazer isso a cada
     * reinício da aplicação.
     *
     * @param groupId identificador do grupo.
     * @param nomeRole nome da realm role (por exemplo {@code ADMIN} ou {@code USER}).
     */
    public void mapearRoleNoGrupo( String groupId, String nomeRole ) {
        var realm = keycloak.realm( keycloakProperties.getRealm() );
        var grupo = realm.groups().group( groupId );

        var jaMapeada = grupo.roles().realmLevel().listEffective()
                .stream()
                .anyMatch( role -> nomeRole.equals( role.getName() ) );

        if ( jaMapeada ) {
            return;
        }

        var role = realm.roles().get( nomeRole ).toRepresentation();
        grupo.roles().realmLevel().add( List.of( role ) );
    }

    /**
     * Remove uma realm role mapeada <b>diretamente</b> no grupo.
     *
     * <p><b>Este método é a exceção deliberada ao contrato do {@link #mapearRoleNoGrupo}</b>, que
     * promete nunca remover mapeamento. Ele existe para um único propósito: aposentar a role
     * uma role legada dos grupos de perfil. Sem ele, a aposentadoria seria
     * apenas nominal — {@code USER} continuaria mapeada em todo grupo, para sempre, porque nada mais
     * no sistema a remove.
     *
     * <h2>Por que consulta listAll() e não listEffective()</h2>
     * O método vizinho {@link #mapearRoleNoGrupo} usa {@code listEffective()}, e a assimetria é
     * proposital — não é descuido de quem escreveu depois.
     *
     * <p>{@code listEffective()} devolve o mapeamento <b>efetivo</b>, que <b>inclui os filhos das
     * roles compostas</b>. Isso foi medido no spike de 2026-08-26: para um grupo mapeado a uma única
     * composta de 30 ações, o mapeamento direto devolveu <b>1</b> role e o efetivo devolveu
     * <b>31</b>. Para <i>mapear</i>, o efetivo é o que se quer — não faz sentido acrescentar
     * diretamente uma role que o grupo já possui por herança.
     *
     * <p>Para <i>remover</i>, o efetivo é a resposta errada. A API do Keycloak só remove o que está
     * mapeado diretamente; pedir a remoção de uma role que o grupo possui apenas por herança da
     * composta não faz nada — ou pior, dá a impressão de ter funcionado. Por isso aqui se consulta
     * {@code listAll()}, o mapeamento <b>direto</b>.
     *
     * <p>É idempotente: role não mapeada diretamente é ignorada em silêncio.
     *
     * @param groupId identificador do grupo.
     * @param nomeRole nome da realm role a remover do mapeamento direto.
     */
    public void desmapearRoleNoGrupo( String groupId, String nomeRole ) {
        var realm = keycloak.realm( keycloakProperties.getRealm() );
        var grupo = realm.groups().group( groupId );

        var mapeadaDiretamente = grupo.roles().realmLevel().listAll()
                .stream()
                .filter( role -> nomeRole.equals( role.getName() ) )
                .findFirst();

        if ( mapeadaDiretamente.isEmpty() ) {
            return;
        }

        grupo.roles().realmLevel().remove( List.of( mapeadaDiretamente.get() ) );
    }

}
