package br.com.gems.app.model.entity.localidades;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Formula;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table( name = "ESTADO" )
@NamedEntityGraph(
        name = "estado-entity-graph",
        attributeNodes = {
                @NamedAttributeNode( value = "pais" ),
        }
)
public class Estado {

    @Id
    @GeneratedValue( strategy = GenerationType.UUID )
    @Column( name = "ID_ESTADO" )
    private UUID id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn( name = "ID_PAIS", nullable = false )
    private Pais pais;

    @Column( name = "NM_ESTADO", nullable = false )
    private String nome;

    @Column( name = "NM_SIGLA_ESTADO", nullable = false, length = 2 )
    private String siglaEstado;

}
