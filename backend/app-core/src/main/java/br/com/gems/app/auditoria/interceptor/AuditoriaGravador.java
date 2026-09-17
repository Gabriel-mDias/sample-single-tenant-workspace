package br.com.gems.app.auditoria.interceptor;

import br.com.gems.app.model.entity.auditoria.CampoAlterado;
import br.com.gems.app.model.entity.auditoria.RegistroAuditoria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Grava a trilha acumulada pelo {@link AuditoriaInterceptor}, no {@code beforeCommit}.
 *
 * <h2>Por que é uma classe separada</h2>
 * O interceptador é construído junto da {@code EntityManagerFactory}. Se ele próprio dependesse de
 * um {@code EntityManager}, o contexto do Spring fecharia um ciclo na inicialização. A separação
 * permite que o interceptador o alcance por {@code ObjectProvider}, resolvido tarde — quando a
 * fábrica já existe e a primeira transação está em curso.
 *
 * <h2>Por que grava por JPA, e não por SQL direto</h2>
 * SQL direto eliminaria qualquer risco de reentrada no interceptador, o que é tentador. Mas
 * duplicaria os nomes das colunas num segundo lugar, fora do alcance do {@code ddl-auto: validate} —
 * e o gate de paridade entre JPA e Liquibase é justamente o que impede a trilha de silenciosamente
 * divergir do schema. A reentrada é resolvida de forma explícita e testada, no
 * {@code deveAuditar} do interceptador; a divergência de schema não teria rede nenhuma.
 */
@Component
public class AuditoriaGravador {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Força a detecção de sujeira antes de a trilha ser lida.
     *
     * <p>
     * Sem ele, uma transação que apenas <b>altera</b> uma entidade pode não ter passado por flush
     * nenhum até o {@code beforeTransactionCompletion}, e o {@code onFlushDirty} — o único callback
     * que entrega o estado anterior — ainda não teria disparado. Em transação sem nada sujo é uma
     * operação sem custo.
     */
    public void forcarFlush() {
        entityManager.flush();
    }

    /**
     * Persiste tudo o que a transação acumulou.
     *
     * <p>
     * O {@code flush} ao final é deliberado: ele reentra no interceptador para as entidades da
     * própria trilha, que são recusadas ali. Fazer isso <b>agora</b>, e não deixar para o commit, é o
     * que mantém o erro — se um dia houver um — dentro da transação, onde ele derruba a operação
     * inteira em vez de deixar dado de negócio gravado sem rastro.
     */
    public void gravar( List<RegistroPendente> pendentes ) {
        for ( var pendente : pendentes ) {
            var registro = RegistroAuditoria.builder()
                    .entidade( pendente.entidade() )
                    .registroAfetado( pendente.registroAfetado() )
                    .operacao( pendente.operacao() )
                    .dataOperacao( pendente.dataOperacao() )
                    .autorId( pendente.autorId() )
                    .autorNome( pendente.autorNome() )
                    .correlacaoId( pendente.correlacaoId() )
                    .build();

            entityManager.persist( registro );

            for ( var campo : pendente.campos() ) {
                entityManager.persist( CampoAlterado.builder()
                        .registroAuditoria( registro )
                        .campo( campo.campo() )
                        .valorAnterior( campo.valorAnterior() )
                        .valorNovo( campo.valorNovo() )
                        .sigiloso( campo.sigiloso() )
                        .build() );
            }
        }

        entityManager.flush();
    }
}
