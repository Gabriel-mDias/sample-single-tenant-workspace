package br.com.gems.{app}.{modulo}.repository;

import br.com.gems.jpa.repository.BaseCustomJpaRepository;
import br.com.gems.utils.ObjectUtil;
import br.com.gems.{app}.{modulo}.dto.{Entidade}FilterParams;
import br.com.gems.{app}.{modulo}.dto.{Entidade}ResponseDTO;
import br.com.gems.{app}.{modulo}.entity.{Entidade};
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

// Trinca obrigatória: search (público) + countQuery (privado) + searchQuery (privado) + appendFilters (privado)
@Repository
public interface {Entidade}Repository extends BaseCustomJpaRepository<{Entidade}> {

    default Page<{Entidade}ResponseDTO> search({Entidade}FilterParams filter, Pageable pageable) {
        return new PageImpl<>(searchQuery(filter, pageable), pageable, countQuery(filter));
    }

    private Long countQuery({Entidade}FilterParams filter) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append(" SELECT count(e.id) FROM {Entidade} e ");
        appendFilters(filter, hql, params);

        return this.executeCountHql(hql, params);
    }

    private List<{Entidade}ResponseDTO> searchQuery({Entidade}FilterParams filter, Pageable pageable) {
        var hql = new StringBuilder();
        var params = new HashMap<String, Object>();

        hql.append(" SELECT new br.com.gems.{app}.{modulo}.dto.{Entidade}ResponseDTO( ");
        hql.append("   e.id, e.nome, e.dataCriacao ");
        hql.append(" ) FROM {Entidade} e ");

        appendFilters(filter, hql, params);

        return this.executeHql(hql, params, pageable, {Entidade}ResponseDTO.class);
    }

    private void appendFilters({Entidade}FilterParams filter, StringBuilder hql, HashMap<String, Object> params) {
        hql.append(" WHERE 1=1 ");

        if (ObjectUtil.isNotNullAndNotEmpty(filter.getNome())) {
            hql.append(" AND LOWER(e.nome) LIKE :nome ");
            params.put("nome", "%" + filter.getNome().toLowerCase() + "%");
        }

        // TODO: adicionar filtros de range, enum, datas conforme rules/backend-java/repositories.md
    }
}
