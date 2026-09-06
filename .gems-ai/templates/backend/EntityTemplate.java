package br.com.gems.{app}.{modulo}.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table( name = "{TABELA}" )
@SQLDelete( sql = "UPDATE {TABELA} SET DT_EXCLUSAO = NOW() WHERE ID_{TABELA} = ?" )
@SQLRestriction( "DT_EXCLUSAO IS NULL" )
/**
 * Por padrão, definir um NamedEntityGraph com os relacionamentos e seus sub_relacionamentos com demais entidades.
 */
@NamedEntityGraph(
        name = "{entidade}-entity-graph",
        attributeNodes = {
                @NamedAttributeNode( value = "{entidadeRelacionada}", subgraph = "{entidadeRelacionada}-subgraph" ),
        },
        subgraphs = {
                @NamedSubgraph( name = "{entidadeRelacionada}-subgraph",
                        attributeNodes = {
                            @NamedAttributeNode( value = "{sub_relacionamento_1}" ),
                            @NamedAttributeNode( value = "{sub_relacionamento_2}" ),
                        }
                ),
        }
)
public class {Entidade} {

    @Id
    @GeneratedValue( strategy = GenerationType.UUID )
    @Column( name = "ID_{TABELA}" )
    private UUID id;

    @Column( name = "DS_PRIMEIRO_NOME", nullable = false )
    private String primeiroNome;

    @Column( name = "DS_SOBRENOME", nullable = false )
    private String sobrenome;

    /**
     * Enumerações: **Sempre** mapear utilizando String. **Nunca** mapear utilizando int ou char.
     */
    @Enumerated( EnumType.STRING )
    @Column( name = "CD_TIPO_DOCUMENTO", nullable = false )
    private TipoDocumentoEnum tipoDocumento;

    /**
     * Campos de texto longos: **Sempre** mapear utilizando Varchar (ex: 4000 caracteres).
     * Mapear utilizando TEXT somente em casos onde for explicitada a necessidade, ou em armazenamento de templates.
     */
    @Column( name = "DS_OBSERVACOES", length = 4000 )
    private String observacoes;

    /**
     * Relacionamentos padrões: Preferencialmente mapear somente no lado que possui a chave estrangeira.
     * Somente mapear a collection no outra entidade quando for explicito a necessidade. 
     */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_{ENTIDADE_RELACIONADA}", nullable = false)
    private {EntidadeRelacionada} {entidadeRelacionada};


    /**
     * Relacionamentos de extrema dependência:
     * Realizar esse mapeamento somente em cenários onde for explicitado que necessita de uma relação
     * tratada com persistência em cascata. Evitar ao máximo! Isso gera acoplamento desnecessário e dificulta
     * a manutenção.
     */
    @ToString.Exclude
    @OneToMany( mappedBy = "{entidadeRelacionada}", cascade = CascadeType.ALL, orphanRemoval = true )
    private List<{EntidadeRelacionada}> {entidadesRelacionadas};

    /**
     * Propriedade virtual calculada diretamente pelo banco (não persistida).
     * Evite recálculos manuais na Service ou DTO para junções simples.
     */
    @Formula( " CONCAT(DS_PRIMEIRO_NOME, ' ', DS_SOBRENOME) " )
    private String nomeCompleto;
    
    // TODO: adicionar outros campos

    @Column( name = "DT_EXCLUSAO" )
    private LocalDateTime dataExclusao;
}
