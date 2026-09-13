// origem: local; precedencia: local-vence; versao: 2.0.0
package br.com.gems.{app}.{modulo}.service;

import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.ObjectUtil;
import br.com.gems.{app}.{modulo}.dto.{Entidade}DTO;
import br.com.gems.{app}.{modulo}.dto.{Entidade}FilterParams;
import br.com.gems.{app}.{modulo}.dto.{Entidade}ResponseDTO;
import br.com.gems.{app}.{modulo}.entity.{Entidade};
import br.com.gems.{app}.{modulo}.repository.{Entidade}Repository;
import lombok.RequiredArgsConstructor;
import br.com.gems.{app}.{modulo}.mapper.{Entidade}Mapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class {Entidade}Service {

    private final {Entidade}Mapper mapper;
    private final {Entidade}Repository repository;
    // private final ApplicationEventPublisher applicationEventPublisher;  // descomentar se usar eventos

    // Ordem obrigatória: findById(String) > findById(UUID) > search > delete > insert > save > validate

    public {Entidade}DTO findById(String id) {
        return findById(UUID.fromString(id));
    }

    public {Entidade}DTO findById(UUID id) {
        {Entidade} entidade = repository.findById(id)
            .orElseThrow(() -> new BusinessException("{Entidade} não encontrado."));
        return mapper.toResponse(entidade);
    }

    public Page<{Entidade}ResponseDTO> search({Entidade}FilterParams filter, Pageable pageable) {
        return repository.search(filter, pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        {Entidade} entidade = repository.findById(UUID.fromString(id))
            .orElseThrow(() -> new BusinessException("{Entidade} não encontrado."));
        repository.delete(entidade);
    }

    @Transactional(rollbackFor = Exception.class)
    public {Entidade}DTO insert({Entidade}DTO dto) {
        validate(dto);
        {Entidade} entidade = mapper.toEntity(dto);
        repository.save(entidade);
        // applicationEventPublisher.publishEvent(new {Entidade}CriadoEvent(entidade.getId()));
        return mapper.toResponse(entidade);
    }

    @Transactional(rollbackFor = Exception.class)
    public {Entidade}DTO save(String id, {Entidade}DTO dto) {
        validate(dto);
        {Entidade} entidade = repository.findById(UUID.fromString(id))
            .orElseThrow(() -> new BusinessException("{Entidade} não encontrado."));
        mapper.atualizar(dto, entidade);
        repository.save(entidade);
        return mapper.toResponse(entidade);
    }

    private void validate({Entidade}DTO dto) {
        List<String> errors = new ArrayList<>();

        if (ObjectUtil.isNullOrEmpty(dto.getNome()))
            errors.add("Nome é obrigatório.");

        // TODO: adicionar regras de negócio específicas

        if (!errors.isEmpty())
            throw new BusinessException(errors);
    }
}
