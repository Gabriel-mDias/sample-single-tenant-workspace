package br.com.gems.{app}.{modulo}.controller;

import br.com.gems.exception.base.BaseController;
import br.com.gems.rest.dto.ApiResponseDTO;
import br.com.gems.{app}.{modulo}.dto.{Entidade}DTO;
import br.com.gems.{app}.{modulo}.dto.{Entidade}FilterParams;
import br.com.gems.{app}.{modulo}.dto.{Entidade}ResponseDTO;
import br.com.gems.{app}.{modulo}.service.{Entidade}Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/{recurso}")
@RequiredArgsConstructor
@Tag(name = "{Entidade}", description = "Gerenciamento de {Entidade}")
public class {Entidade}Controller extends BaseController {

    private final {Entidade}Service service;

    @GetMapping("/{id}")
    @Operation(summary = "Buscar {entidade} por ID")
    public ResponseEntity<ApiResponseDTO<{Entidade}DTO>> findById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponseDTO.of(service.findById(id)));
    }

    @PostMapping("/search")
    @Operation(summary = "Pesquisar {entidades} com filtros e paginação")
    public ResponseEntity<ApiResponseDTO<Page<{Entidade}ResponseDTO>>> search(
            @RequestBody {Entidade}FilterParams filter,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.of(service.search(filter, pageable)));
    }

    @PostMapping
    @Operation(summary = "Criar {entidade}")
    public ResponseEntity<ApiResponseDTO<{Entidade}DTO>> insert(@Valid @RequestBody {Entidade}DTO dto) {
        setBodyToExceptionLog(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.of(service.insert(dto)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar {entidade}")
    public ResponseEntity<ApiResponseDTO<{Entidade}DTO>> save(
            @PathVariable String id,
            @Valid @RequestBody {Entidade}DTO dto) {
        setBodyToExceptionLog(dto);
        return ResponseEntity.ok(ApiResponseDTO.of(service.save(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir {entidade}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
