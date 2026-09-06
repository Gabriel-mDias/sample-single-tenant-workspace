package br.com.gems.app.localidade.repository;

import br.com.gems.app.model.entity.localidades.Pais;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso ao catálogo de países.
 *
 * <p>{@code Pais} é dado de referência: carregado por changeset de seed, nunca editado pelo usuário
 * e sem soft delete (isenção prevista no Princípio II da constituição). Por isso este repositório
 * não estende {@code BaseCustomJpaRepository} — não há busca paginada nem filtro composto, apenas
 * leitura para alimentar os selects do wizard.
 */
@Repository
public interface PaisRepository extends JpaRepository<Pais, UUID> {

    List<Pais> findAllByOrderByNomeAsc();

    /** Resolve o país pelo nome — usado pela consulta de CEP, que sempre devolve endereços no Brasil. */
    Optional<Pais> findByNomeIgnoreCase( String nome );

}
