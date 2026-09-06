package br.com.gems.app.auditoria.interceptor;

import br.com.gems.app.model.entity.base.CampoSigiloso;
import br.com.gems.app.model.entity.base.EntidadeAuditavel;
import br.com.gems.app.model.enums.OperacaoAuditoria;
import br.com.gems.app.security.service.UsuarioAutenticadoService;
import br.com.gems.rest.filter.CorrelationIdFilter;
import org.hibernate.Hibernate;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.type.Type;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * O mecanismo da trilha de auditoria (o requisito a o requisito). 
 *
 * <h2>Nenhum serviço de domínio o chama — e esse é o requisito</h2>
 * O requisito exige que o registro seja produzido pela camada de persistência, nunca por uma linha
 * dentro de cada {@code save}. A alternativa foi avaliada e recusada pelo motivo mais banal que
 * existe: uma linha que precisa ser lembrada em cada serviço novo <b>será esquecida</b>. Aqui, uma
 * entidade criada em qualquer rodada futura nasce auditada sem que ninguém decida nada.
 *
 * <h2>Como ele entra no Hibernate</h2>
 * Implementando {@link HibernatePropertiesCustomizer}, ele se injeta como interceptador da
 * <i>SessionFactory</i>. Isso é o que permite que ele seja um <b>bean do Spring</b>, com injeção de
 * dependência — declarar a classe por propriedade de configuração daria uma instância criada pelo
 * Hibernate, sem acesso a nada.
 *
 * <p><b>Consequência: a instância é única e compartilhada por todas as threads.</b> Por isso o
 * acúmulo é {@link ThreadLocal} — e por isso ele é sempre limpo em {@code afterCompletion}, inclusive
 * quando a transação falha. Um {@code ThreadLocal} não limpo em pool de threads vaza dado de uma
 * requisição para a seguinte, que aqui significaria atribuir a alteração de alguém a outra pessoa.
 *
 * <h2>As três armadilhas, e o que fecha cada uma</h2>
 * <ol>
 *   <li><b>Recursão.</b> Gravar a trilha é, ele próprio, um {@code persist}. Sem guarda, a trilha
 *       geraria trilha da trilha, sem fim. Fecha em {@link #deveAuditar(Object)}, com teste.</li>
 *   <li><b>Transação.</b> A gravação acontece no {@code beforeCommit}, ainda dentro da transação do
 *       dado. É o rollback do dado é rollback da trilha, e há teste que o prova.</li>
 *   <li><b>Estado anterior.</b> Só o {@code onFlushDirty} do Hibernate entrega
 *       {@code previousState}. Fora dele, "o que mudou" é uma pergunta sem resposta barata.</li>
 * </ol>
 *
 * <h2>O que NÃO é auditado, e por quê</h2>
 * Propriedades de associação — {@code @ManyToOne} e coleções — são puladas. Duas razões, e a segunda
 * é a que importa: elas são <b>lazy</b>, e tocá-las durante o flush para descobrir o valor anterior
 * dispararia carregamento no meio da varredura do Hibernate. E não se perde quase nada: os agregados
 * deste projeto ({@code Contato}, {@code Endereco}, {@code Pessoa}) são eles próprios entidades
 * auditáveis e produzem o <b>seu</b> registro, amarrado ao mesmo identificador de correlação.
 *
 * @see CampoSigiloso
 * @see AuditoriaGravador
 */
@Component
public class AuditoriaInterceptor implements Interceptor, HibernatePropertiesCustomizer {

    /**
     * Cinto e suspensório. A guarda que realmente exclui a trilha de si mesma é o
     * {@code instanceof EntidadeAuditavel} — as entidades de auditoria não a estendem. Este pacote é
     * a segunda tranca, para o dia em que alguém fizer {@code RegistroAuditoria} estender a base sem
     * perceber a consequência.
     */
    private static final String PACOTE_AUDITORIA = "br.com.gems.app.model.entity.auditoria";

    /** O autor de toda operação sem JWT no contexto: o bootstrap, um teste, uma rotina. */
    private static final String AUTOR_SISTEMA = "sistema";

    private static final ThreadLocal<List<RegistroPendente>> PENDENTES =
            ThreadLocal.withInitial( ArrayList::new );

    private final ObjectProvider<AuditoriaGravador> gravador;
    private final ObjectProvider<UsuarioAutenticadoService> usuarioAutenticado;

    /**
     * Os dois vêm por {@link ObjectProvider} de propósito: este bean é consumido na construção da
     * {@code EntityManagerFactory}, e exigir o {@code AuditoriaGravador} — que depende de um
     * {@code EntityManager} — de forma ansiosa fecharia um ciclo na inicialização do contexto.
     */
    public AuditoriaInterceptor( ObjectProvider<AuditoriaGravador> gravador,
                                 ObjectProvider<UsuarioAutenticadoService> usuarioAutenticado ) {
        this.gravador = gravador;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @Override
    public void customize( Map<String, Object> hibernateProperties ) {
        hibernateProperties.put( AvailableSettings.INTERCEPTOR, this );
    }

    /**
     * <b>A gravação acontece aqui, e o caminho até esta escolha custou um teste vermelho.</b>
     *
     * <p>A primeira versão gravava no {@code beforeCommit} de uma {@code TransactionSynchronization}
     * do Spring. Funcionava para inserção e exclusão, e <b>perdia toda alteração</b>. A razão é de
     * ordem de execução: {@code onPersist} e {@code onRemove} disparam no instante da chamada, ainda
     * no corpo da transação, mas {@code onFlushDirty} só dispara no <b>flush</b> — e o flush do
     * commit acontece <i>depois</i> de o Spring já ter disparado o {@code beforeCommit}. Numa
     * transação que apenas altera, o acúmulo nascia tarde demais e era descartado.
     *
     * <p>Registrar a sincronização em {@code afterTransactionBegin} também não resolve: medido, o
     * {@code TransactionSynchronizationManager} ainda <b>não está ativo</b> naquele ponto, porque o
     * {@code JpaTransactionManager} inicia a transação antes de preparar a sincronização.
     *
     * <p>Este callback do próprio Hibernate é o ponto certo: ele roda <b>depois</b> do flush final e
     * <b>antes</b> do commit físico, ainda dentro da transação. É onde o Hibernate Envers grava a
     * trilha dele, pelos mesmos motivos. E é o que faz o requisito valer sem depender de nada do Spring.
     */
    @Override
    public void beforeTransactionCompletion( Transaction tx ) {
        if ( PENDENTES.get().isEmpty() ) {
            // Ainda assim é preciso flushar: numa transação que só ALTERA, o onFlushDirty pode não
            // ter disparado até aqui, e é ele quem preenche o acúmulo.
            gravador.getObject().forcarFlush();
        }

        if ( PENDENTES.get().isEmpty() ) {
            return;
        }

        gravador.getObject().gravar( List.copyOf( PENDENTES.get() ) );
    }

    /**
     * SEMPRE, inclusive em rollback: {@code ThreadLocal} não limpo em pool de threads vaza dado de
     * uma requisição para a seguinte — que aqui significaria atribuir a alteração de alguém a outra
     * pessoa.
     */
    @Override
    public void afterTransactionCompletion( Transaction tx ) {
        PENDENTES.remove();
    }

    @Override
    public boolean onPersist( Object entity, Object id, Object[] state, String[] propertyNames,
                              Type[] types ) {
        if ( deveAuditar( entity ) ) {
            acumular( entity, id, OperacaoAuditoria.INSERCAO, List.of() );
        }

        // false: não alteramos o state da entidade. A trilha observa, não interfere.
        return false;
    }

    @Override
    public boolean onFlushDirty( Object entity, Object id, Object[] currentState,
                                 Object[] previousState, String[] propertyNames, Type[] types ) {
        if ( !deveAuditar( entity ) || previousState == null ) {
            return false;
        }

        var campos = camposAlterados( entity, currentState, previousState, propertyNames, types );

        // Um flush pode marcar a entidade como suja sem que nenhuma coluna auditável tenha mudado —
        // o caso mais comum é só uma associação ter sido trocada. Registro sem campo é ruído.
        if ( !campos.isEmpty() ) {
            acumular( entity, id, OperacaoAuditoria.ALTERACAO, campos );
        }

        return false;
    }

    @Override
    public void onRemove( Object entity, Object id, Object[] state, String[] propertyNames,
                          Type[] types ) {
        if ( deveAuditar( entity ) ) {
            // Chega aqui inclusive na exclusão LÓGICA: o @SQLDelete troca o SQL da operação, não a
            // natureza dela — para o Hibernate continua sendo uma remoção.
            acumular( entity, id, OperacaoAuditoria.EXCLUSAO, List.of() );
        }
    }

    // ------------------------------------------------------------------ regras

    /**
     * <b>Entidade de negócio é toda subclasse de {@link EntidadeAuditavel}</b> — decisão D8 da
     * rodada, e ela resolve três coisas de uma vez.
     *
     * <p>Primeiro, dá ao requisito uma fronteira que não é lista escrita à mão: uma entidade nova nasce
     * auditada por estender a base, que é o que ela já faz por convenção. Segundo, exclui
     * automaticamente {@code Pais} e {@code Estado} — as duas tabelas puramente referenciais, que o
     * Princípio II já isenta e cuja carga por seed encheria a trilha de ruído. Terceiro, exclui a
     * própria trilha, que deliberadamente não estende a base.
     *
     * <p><b>Visibilidade de pacote, e não privada, de propósito.</b> A guarda de recursão precisa de
     * um teste que a exercite <i>diretamente</i>. O teste de integração que conta "trilha da trilha"
     * não serve sozinho: uma prova por mutação mostrou que ele passa mesmo com esta guarda desligada,
     * porque o gravador consome uma cópia do acúmulo numa passada só, e os registros da trilha
     * bufferizados durante a escrita nunca chegam a ser gravados. Rede que não distingue os dois
     * casos não é rede — é a mesma lição do {@code routes-coverage.spec.ts}.
     */
    boolean deveAuditar( Object entity ) {
        if ( !( entity instanceof EntidadeAuditavel ) ) {
            return false;
        }

        return !Hibernate.getClass( entity ).getPackageName().equals( PACOTE_AUDITORIA );
    }

    private List<RegistroPendente.CampoPendente> camposAlterados( Object entity,
                                                                  Object[] currentState,
                                                                  Object[] previousState,
                                                                  String[] propertyNames,
                                                                  Type[] types ) {
        var campos = new ArrayList<RegistroPendente.CampoPendente>();

        for ( int i = 0; i < propertyNames.length; i++ ) {
            if ( types[ i ].isAssociationType() || Objects.equals( previousState[ i ], currentState[ i ] ) ) {
                continue;
            }

            var sigiloso = ehSigiloso( Hibernate.getClass( entity ), propertyNames[ i ] );

            campos.add( new RegistroPendente.CampoPendente(
                    propertyNames[ i ],
                    sigiloso ? null : texto( previousState[ i ] ),
                    sigiloso ? null : texto( currentState[ i ] ),
                    sigiloso ) );
        }

        return campos;
    }

    /**
     * Procura a anotação subindo a hierarquia, porque o campo pode estar numa superclasse mapeada.
     * Campo não encontrado é tratado como não sigiloso: a única forma de um nome de propriedade não
     * ter campo correspondente é ser derivado, e derivado não guarda segredo.
     */
    private boolean ehSigiloso( Class<?> tipo, String propriedade ) {
        for ( var classe = tipo; classe != null && classe != Object.class; classe = classe.getSuperclass() ) {
            try {
                return classe.getDeclaredField( propriedade ).isAnnotationPresent( CampoSigiloso.class );
            } catch ( NoSuchFieldException procuraNaSuperclasse ) {
                // segue subindo
            }
        }

        return false;
    }

    private String texto( Object valor ) {
        return valor == null ? null : String.valueOf( valor );
    }

    // ------------------------------------------------------------- acumulação

    private void acumular( Object entity, Object id, OperacaoAuditoria operacao,
                           List<RegistroPendente.CampoPendente> campos ) {
        if ( !( id instanceof UUID identificador ) ) {
            // Toda entidade do projeto tem chave UUID. Um id de outro tipo significa mapeamento que
            // esta trilha não sabe endereçar, e registrar lixo é pior que não registrar.
            return;
        }

        var autorId = usuarioAutenticado.getObject().idDoUsuarioAutenticado().orElse( null );
        var autorNome = usuarioAutenticado.getObject().loginDoUsuarioAutenticado()
                .orElse( AUTOR_SISTEMA );

        PENDENTES.get().add( new RegistroPendente(
                Hibernate.getClass( entity ).getSimpleName(),
                identificador,
                operacao,
                autorId,
                autorNome,
                MDC.get( CorrelationIdFilter.MDC_KEY ),
                LocalDateTime.now(),
                campos ) );
    }

}
