package br.com.gems.app.model.entity.localidades;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table( name = "PAIS" )
public class Pais {

    @Id
    @GeneratedValue( strategy = GenerationType.UUID )
    @Column( name = "ID_PAIS" )
    private UUID id;

    @Column( name = "NM_PAIS", nullable = false )
    private String nome;

}
