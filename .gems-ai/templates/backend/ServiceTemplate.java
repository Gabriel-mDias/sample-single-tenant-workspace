package br.com.gems.{app}.{modulo}.service;

import br.com.gems.exception.exception.BusinessException;
import br.com.gems.utils.ObjectUtil;
import br.com.gems.{app}.{modulo}.dto.{Entidade}DTO;
import br.com.gems.{app}.{modulo}.dto.{Entidade}FilterParams;
import br.com.gems.{app}.{modulo}.dto.{Entidade}ResponseDTO;
import br.com.gems.{app}.{modulo}.entity.{Entidade};
import br.com.gems.{app}.{modulo}.repository.{Entidade}Repository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
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

    private final ModelMapper modelMapper;
    private final {Entidade}Repository repository;
    // private final ApplicationEventPublisher applicationEventPublisher;  // descomentar se usar eventos

    // Ordem obrigatória: findById(String) > findById(UUID) > search > delete > insert > save > validate

    public {Entidade}DTO findById(String id) {
        return findById(UUID.fromString(id));
    }

    public {Entidade}DTO findById(UUID id) {
        {Entidade} entidade = repository.findById(id)
            .orElseThrow(() -> new BusinessException("{Entidade} não encontrado."));
        return modelMapper.map(entidade, {Entidade}DTO.class);
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
        {Entidade} entidade = modelMapper.map(dto, {Entidade}.class);
        repository.save(entidade);
        // applicationEventPublisher.publishEvent(new {Entidade}CriadoEvent(entidade.getId()));
        return modelMapper.map(entidade, {Entidade}DTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public {Entidade}DTO save(String id, {Entidade}DTO dto) {
        validate(dto);
        {Entidade} entidade = repository.findById(UUID.fromString(id))
            .orElseThrow(() -> new BusinessException("{Entidade} não encontrado."));
        modelMapper.map(dto, entidade);
        repository.save(entidade);
        return modelMapper.map(entidade, {Entidade}DTO.class);
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
