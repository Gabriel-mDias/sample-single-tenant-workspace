# Recipe: CRUD Ponta-a-Ponta

**Objetivo:** Implementar um CRUD completo desde a entidade JPA até o formulário Angular, cobrindo todas as camadas da GEMS SDK.

**Pré-requisitos:**
- `gems-jpa` (BaseCustomJpaRepository, soft delete)
- `gems-model-mapper` (ModelMapper)
- `gems-rest-common` (ApiResponseDTO, BaseController)
- `gems-exception` (BusinessException)
- `gems-sdk` Angular (GemsBaseStore, FormCardComponent, TableComponent)

**Rules relacionadas:**
- [architecture.md](../rules/backend-java/architecture.md)
- [controllers.md](../rules/backend-java/controllers.md)
- [services.md](../rules/backend-java/services.md)
- [repositories.md](../rules/backend-java/repositories.md)
- [persistence-db.md](../rules/backend-java/persistence-db.md)
- [mapping.md](../rules/backend-java/mapping.md)
- [stores.md](../rules/frontend-angular/stores.md)
- [lists.md](../rules/frontend-angular/lists.md)
- [forms.md](../rules/frontend-angular/forms.md)

## Resultado Esperado

- Entidade JPA persistida com soft delete, prefixos GEMS nas colunas e UUID como PK.
- Service com `ModelMapper` como primeiro campo, `@Transactional` nos métodos de escrita e `BusinessException` em falhas de negócio.
- Listagem Angular com `gems-table` paginada via `POST /search` e formulário com `gems-form-card` distinguindo criação de edição.

---

## Camada 1: Entidade JPA

```java
@Entity
@Table(name = "TB_{ENTIDADE}")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@SQLDelete(sql = "UPDATE TB_{ENTIDADE} SET ST_ATIVO = false WHERE ID_{ENTIDADE} = ?")
@SQLRestriction("ST_ATIVO = true")
public class {Entidade} {

    @Id
    @Column(name = "ID_{ENTIDADE}", columnDefinition = "VARCHAR(36)")
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "DS_NOME", nullable = false, length = 100)
    private String nome;

    @Column(name = "ST_ATIVO", nullable = false)
    private Boolean ativo = true;

    @Column(name = "DT_CRIACAO", updatable = false)
    @CreationTimestamp
    private LocalDateTime dataCriacao;

    @Column(name = "DT_ATUALIZACAO")
    @UpdateTimestamp
    private LocalDateTime dataAtualizacao;
}
```

## Camada 2: DTOs

```java
// {Entidade}DTO.java — espelho (create/update)
@Data
public class {Entidade}DTO {
    private String id;
    private String nome;
}

// {Entidade}ResponseDTO.java — somente para listagem
@Data
public class {Entidade}ResponseDTO {
    private String id;
    private String nome;
    private LocalDateTime dataCriacao;
}

// {Entidade}FilterParams.java — filtros do search
@Data
public class {Entidade}FilterParams {
    private String nome;
}
```

## Camada 3: Repository

```java
public interface {Entidade}Repository extends BaseCustomJpaRepository<{Entidade}> {

    Page<{Entidade}ResponseDTO> search(
        @Param("nome") String nome,
        Pageable pageable
    );

    @Query(value = "SELECT e FROM {Entidade} e WHERE 1=1 /*filter*/", countQuery = "SELECT COUNT(e) FROM {Entidade} e WHERE 1=1 /*filter*/")
    Page<{Entidade}ResponseDTO> searchQuery(
        @Param("nome") String nome,
        Pageable pageable
    );

    default Page<{Entidade}ResponseDTO> search({Entidade}FilterParams filter, Pageable pageable) {
        return searchQuery(filter.getNome(), pageable);
    }
}
```

## Camada 4: Service

```java
@Service
@RequiredArgsConstructor
public class {Entidade}Service {

    private final ModelMapper modelMapper;
    private final {Entidade}Repository repository;

    public {Entidade} findById(String id) {
        return findById(UUID.fromString(id));
    }

    public {Entidade} findById(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new BusinessException("{Entidade} não encontrado."));
    }

    public Page<{Entidade}ResponseDTO> search({Entidade}FilterParams filter, Pageable pageable) {
        return repository.search(filter.getNome(), pageable);
    }

    @Transactional(rollbackOn = Exception.class)
    public {Entidade} insert({Entidade}DTO dto) {
        validate(dto);
        {Entidade} entidade = modelMapper.map(dto, {Entidade}.class);
        return repository.save(entidade);
    }

    @Transactional(rollbackOn = Exception.class)
    public {Entidade} save(String id, {Entidade}DTO dto) {
        validate(dto);
        {Entidade} entidade = findById(id);
        modelMapper.map(dto, entidade);
        return repository.save(entidade);
    }

    @Transactional(rollbackOn = Exception.class)
    public void delete(String id) {
        {Entidade} entidade = findById(id);
        repository.delete(entidade);   // Soft delete via @SQLDelete
    }

    private void validate({Entidade}DTO dto) {
        List<String> errors = new ArrayList<>();
        if (ObjectUtil.isNullOrEmpty(dto.getNome()))
            errors.add("Nome é obrigatório.");
        if (!errors.isEmpty())
            throw new BusinessException(errors);
    }
}
```

## Camada 5: Controller

```java
@RestController
@RequestMapping("/api/{recurso}")
@RequiredArgsConstructor
@Tag(name = "{Entidade}", description = "Gerenciamento de {Entidade}")
public class {Entidade}Controller extends BaseController {

    private final {Entidade}Service service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<{Entidade}DTO>> findById(@PathVariable String id) {
        {Entidade} entidade = service.findById(id);
        return ResponseEntity.ok(ApiResponseDTO.of(modelMapper.map(entidade, {Entidade}DTO.class)));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponseDTO<Page<{Entidade}ResponseDTO>>> search(
            @RequestBody {Entidade}FilterParams filter,
            @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.of(service.search(filter, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDTO<{Entidade}DTO>> insert(@Valid @RequestBody {Entidade}DTO dto) {
        setBodyToExceptionLog(dto);
        {Entidade} entidade = service.insert(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponseDTO.of(modelMapper.map(entidade, {Entidade}DTO.class)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<{Entidade}DTO>> save(
            @PathVariable String id, @Valid @RequestBody {Entidade}DTO dto) {
        setBodyToExceptionLog(dto);
        {Entidade} entidade = service.save(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.of(modelMapper.map(entidade, {Entidade}DTO.class)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

## Camada 6: Store Angular

```typescript
@Injectable({ providedIn: 'root' })
export class {Entidade}Store extends GemsBaseStore {
  constructor() { super('api/{recurso}'); }

  getById(id: string): Observable<{Entidade}Model> {
    return this.get<{Entidade}Model>(`/${id}`);
  }

  search(filter: {Entidade}FilterModel, pageable: Pageable): Observable<{ content: {Entidade}Model[]; totalElements: number }> {
    return this.post<{ content: {Entidade}Model[]; totalElements: number }, {Entidade}FilterModel>('/search', filter, { pageable });
  }

  create(dto: {Entidade}Model): Observable<{Entidade}Model> {
    return this.post<{Entidade}Model, {Entidade}Model>('', dto);
  }

  update(id: string, dto: {Entidade}Model): Observable<{Entidade}Model> {
    return this.put<{Entidade}Model, {Entidade}Model>(`/${id}`, dto);
  }

  delete{Entidade}(id: string): Observable<void> {
    return this.delete<void>(`/${id}`);
  }
}
```

## Camada 7: Models TypeScript

```typescript
// {entidade}.model.ts
export interface {Entidade}Model {
  id?: string;
  nome?: string;
  dataCriacao?: string;
}

// {entidade}-filter.model.ts
export interface {Entidade}FilterModel {
  nome?: string;
}
```

## Camada 8: Listagem e Formulário Angular

Ver receitas:
- [search-paginated-end-to-end.md](./search-paginated-end-to-end.md) — listagem com filtros e paginação.
- Componente de formulário: [forms.md](../rules/frontend-angular/forms.md).

---

## Checklist de Conformidade

- [ ] Entidade com `@SQLDelete` + `@SQLRestriction` (soft delete).
- [ ] 3 DTOs: espelho (create/update) + ResponseDTO (listagem) + FilterParams (search).
- [ ] Service com ordem: `findById(String)` > `findById(UUID)` > `search` > `delete` > `insert` > `save` > `validate`.
- [ ] `ObjectUtil.isNullOrEmpty()` em toda validação — nunca `== null`.
- [ ] `validate()` acumula erros em `ArrayList<String>` + lança `BusinessException(errors)`.
- [ ] Controller extende `BaseController`, chama `setBodyToExceptionLog(dto)` nos mutantes.
- [ ] `POST /search` para listagem paginada (não `GET`).
- [ ] `POST` retorna `201 CREATED`; `PUT` e `DELETE` retornam `200` e `204`.
- [ ] Store com `super('api/{recurso}')` sem barra inicial.
- [ ] IDs sempre `string` no frontend.
