package br.com.gems.app.security.service;

import br.com.gems.app.security.config.AppKeycloakProperties;
import br.com.gems.exception.exception.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Criação e composição de realm roles no Keycloak.
 *
 * <p>
 * Nasceu ao lado do {@link KeycloakGroupService}, que já sabia criar grupo e mapear
 * role mas <b>não</b> sabia criar role nem montar composta. As duas operações daqui são o que permite
 * que o catálogo de ações viva em código: o {@code AutorizacaoBootstrap} converge o realm a partir do
 * enum a cada início da aplicação, sem que ninguém precise mexer no console administrativo.
 *
 * <p>
 * Segue a mesma forma do serviço vizinho — injeta {@link Keycloak} e
 * {@link AppKeycloakProperties}, e é deliberadamente primitivo: não conhece {@code PerfilAtuacao}
 * nem a matriz de permissões. Quem casa as metades é o bootstrap, no {@code app-core}.
 *
 * <p>
 * <b>Ambas as operações são idempotentes.</b> Rodar duas vezes seguidas produz o mesmo estado e a
 * segunda execução não escreve nada — é o que torna seguro chamá-las a cada boot.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakRoleService {

    private static final List<Integer> STATUS_SUCESSO = List.of( 200, 201, 204 );
    private static final Integer STATUS_CONFLITO = 409;

    private final Keycloak keycloak;
    private final AppKeycloakProperties keycloakProperties;

    /**
     * Garante que a realm role exista, criando-a se necessário.
     *
     * <p>
     * Trata <b>409 como sucesso</b>, pelo mesmo motivo que o {@code criarGrupo} do serviço
     * vizinho: duas instâncias da aplicação subindo ao mesmo tempo disputam a criação, e a que perde
     * a corrida recebe conflito. Isso não é erro — o estado desejado foi alcançado, só não por ela.
     *
     * @param nome
     *            nome da role, em {@code MAIUSCULA_COM_UNDERSCORE}.
     * @param descricao
     *            descrição legível, exibida no console administrativo.
     * @return {@code true} se a role foi criada agora; {@code false} se já existia.
     */
    public boolean garantirRole( String nome, String descricao ) {
        var realm = keycloak.realm( keycloakProperties.getRealm() );

        if ( existeRole( nome ) ) {
            return false;
        }

        var role = new RoleRepresentation();
        role.setName( nome );
        role.setDescription( descricao );
        role.setComposite( false );

        try {
            realm.roles().create( role );
            return true;
        } catch ( jakarta.ws.rs.WebApplicationException e ) {
            var status = e.getResponse().getStatus();

            if ( STATUS_CONFLITO.equals( status ) ) {
                // Outra instância criou a role entre o existeRole() e o create(). Estado desejado
                // alcançado; nada a fazer.
                return false;
            }

            if ( !STATUS_SUCESSO.contains( status ) ) {
                throw new SecurityException(
                        "Falha ao criar a realm role '%s' no Keycloak (HTTP %d).".formatted( nome, status ) );
            }

            return true;
        }
    }

    /**
     * Garante que a role composta exista e contenha <b>exatamente ao menos</b> os filhos informados.
     *
     * <p>
     * Acrescenta o que falta e <b>não remove</b> o que sobra. A escolha é deliberada e espelha o
     * contrato do {@code mapearRoleNoGrupo}: um administrador pode ter acrescentado uma ação a um
     * perfil pelo console para resolver um caso urgente, e o bootstrap não deve desfazer isso a cada
     * reinício. O preço é que remover uma ação da matriz não a remove do realm automaticamente —
     * é consequência conhecida e registrada.
     *
     * @param nome
     *            nome da composta (por exemplo {@code PERFIL_OPERADOR} ou {@code ADMIN}).
     * @param descricao
     *            descrição legível.
     * @param filhos
     *            nomes das roles de ação que a composta deve conter. Pode ser vazio — é o caso de
     *            {@code PERFIL_CONSULTA}, por exemplo, que pode existir sem
     *            nenhuma ação.
     * @return quantidade de filhos <b>acrescentados</b> nesta execução; zero quando nada mudou.
     */
    public int garantirComposta( String nome, String descricao, Set<String> filhos ) {
        var realm = keycloak.realm( keycloakProperties.getRealm() );

        garantirRole( nome, descricao );

        var recurso = realm.roles().get( nome );

        // Só os filhos DIRETOS da composta. getRoleComposites devolve exatamente isso, e é o que se
        // quer aqui: perguntar "o que esta composta declara" e não "o que ela alcança".
        var jaContidos = recurso.getRoleComposites()
                .stream()
                .map( RoleRepresentation::getName )
                .collect( Collectors.toSet() );

        var faltantes = filhos.stream()
                .filter( filho -> !jaContidos.contains( filho ) )
                .map( filho -> realm.roles().get( filho ).toRepresentation() )
                .toList();

        if ( faltantes.isEmpty() ) {
            return 0;
        }

        recurso.addComposites( faltantes );
        return faltantes.size();
    }

    /**
     * @return {@code true} se a realm role já existe.
     */
    private boolean existeRole( String nome ) {
        try {
            keycloak.realm( keycloakProperties.getRealm() ).roles().get( nome ).toRepresentation();
            return true;
        } catch ( jakarta.ws.rs.NotFoundException e ) {
            return false;
        }
    }

}
