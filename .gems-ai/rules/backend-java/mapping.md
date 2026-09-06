---
stack: backend-java
module: gems-model-mapper
severity: mandatory
see-also: [services.md, architecture.md, repositories.md]
---

# Mapeamento Entity ↔ DTO — gems-model-mapper

> **TL;DR:**
> - Use `ModelMapper` (`gems-model-mapper`) via injeção — **nunca** instancie manualmente.
> - `ModelMapper` é o **primeiro campo** injetado na Service.
> - `mapper.map(origem, Destino.class)` para conversão simples.
> - Para atualização: `mapper.map(dto, entityExistente)` — sobrescreve in-place sem criar nova instância.
> - Campos especiais (listas, FKs) podem precisar de mapeamento manual.

## 1. Configuração

O `gems-model-mapper` fornece uma instância pré-configurada de `ModelMapper` como bean Spring. Basta injetá-la:

```java
@Service
@RequiredArgsConstructor
public class {Entidade}Service {
    private final ModelMapper mapper;  // Primeiro campo — sempre
    private final {Entidade}Repository repository;
```

---

## 2. Padrões de Uso

### Conversão Entity → DTO (leitura)
```java
var dto = mapper.map( entity, {Entidade}DTO.class );
```

### Conversão DTO → Entity (criação)
```java
var entity = mapper.map( dto, {Entidade}.class );
entity = repository.save( entity );
```

### Atualização: DTO → Entity existente (PUT)
```java
// Carrega a entidade existente, aplica o DTO sobre ela, preservando campos não mapeados
var entity = repository.findById( id )
        .orElseThrow( () -> new BusinessException( "{Entidade} não encontrado(a)!" ) );
mapper.map( dto, entity );  // Sobrescreve in-place
entity.setId( id );         // Garante que o ID não foi sobrescrito
repository.save( entity );
```

### Stream de lista
```java
var dtos = entities.stream()
        .map( entity -> mapper.map( entity, {Entidade}DTO.class ) )
        .toList();
```

---

## 3. Mapeamento Manual (quando necessário)

Para campos que o ModelMapper não resolve automaticamente (listas aninhadas, chaves FK diretas):

```java
// Referência JPA sem carregar a entidade inteira — evita N+1
if ( dto.getRelacionadoId() != null ) {
    entity.setRelacionado( relacionadoRepository.getReferenceById( dto.getRelacionadoId() ) );
}

// Lista de sub-entidades
entity.setItens( dto.getItens().stream()
        .map( itemDto -> mapper.map( itemDto, Item.class ) )
        .toList() );
```

---

## 4. Regras

- **Nunca** instancie `new ModelMapper()` diretamente — use sempre o bean injetado.
- Para Page: use `.map( entity -> mapper.map( entity, DTO.class ) )` do Spring.
- Campos que o DTO não deve expor (ex: `dataExclusao`) devem estar ausentes no DTO ou marcados com estratégia de skip no bean de configuração.
- Relações bidirecionais complexas podem precisar de `@JsonIgnore` no DTO para evitar ciclos de serialização.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `new ModelMapper()` instanciado manualmente | Injetar o bean `ModelMapper` via `@RequiredArgsConstructor` |
| `var novaEntity = mapper.map(dto, Entidade.class)` para atualização (PUT) | `mapper.map(dto, entityExistente)` — sobrescreve in-place |
| Esquecer `entity.setId(id)` após `mapper.map(dto, entity)` | Sempre setar o ID explicitamente após map no update |
| `repository.findById(UUID.fromString(dto.getId())).get()` para setar FK | `repository.getReferenceById(UUID.fromString(dto.getId()))` |
