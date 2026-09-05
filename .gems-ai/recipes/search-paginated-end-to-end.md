# Recipe: Search Paginado Ponta-a-Ponta

**Objetivo:** Implementar uma consulta paginada completa, da HQL no repositório até a `gems-table` com filtros, paginação, ordenação e skeleton de loading.

**Pré-requisitos:**
- `gems-jpa` (BaseCustomJpaRepository)
- `gems-rest-common` (PageResponseDTO, Pageable)
- `gems-sdk` Angular (TableComponent, FormCardComponent, GemsBaseStore)

**Rules relacionadas:**
- [repositories.md](../rules/backend-java/repositories.md)
- [services.md](../rules/backend-java/services.md)
- [controllers.md](../rules/backend-java/controllers.md)
- [rest-common.md](../rules/backend-java/rest-common.md)
- [stores.md](../rules/frontend-angular/stores.md)
- [lists.md](../rules/frontend-angular/lists.md)
- [gems-components.md](../rules/frontend-angular/gems-components.md)

## Resultado Esperado

- Endpoint `POST /api/{recurso}/search` recebendo `FilterParams` + `Pageable` e retornando `Page<ResponseDTO>`.
- Repository com trinca HQL (`search` default + `searchQuery` @Query + `appendFilters`), sem hardcoded de filtros opcionais.
- Listagem Angular com `gems-table` paginada, ordenável, com filtros via `gems-form-card` e skeleton de loading via `@if (isLoading())`.

---

## Passo 1: ResponseDTO (Backend)

Crie um DTO mínimo para retornar na listagem — sem dados sensíveis ou sub-entidades desnecessárias.

```java
// {Entidade}ResponseDTO.java
@Data
public class {Entidade}ResponseDTO {
    private String id;
    private String nome;
    private String status;    // Enum serializado como String
    private LocalDate dataCriacao;
}
```

## Passo 2: FilterParams (Backend)

```java
// {Entidade}FilterParams.java
@Data
public class {Entidade}FilterParams {
    private String nome;
    private String status;       // null = sem filtro
    private BigDecimal valorMin;
    private BigDecimal valorMax;
    private LocalDate dataInicio;
    private LocalDate dataFim;
}
```

## Passo 3: Repository — Trinca Obrigatória

```java
public interface {Entidade}Repository extends BaseCustomJpaRepository<{Entidade}> {

    // 1. search — retorna página de ResponseDTO
    @Query(value = """
        SELECT new br.com.gems.{app}.{modulo}.dto.{Entidade}ResponseDTO(
            e.id, e.nome, e.status, e.dataCriacao
        )
        FROM {Entidade} e
        WHERE 1=1
        """,
        countQuery = "SELECT count(e) FROM {Entidade} e WHERE 1=1")   // ← countQuery na mesma anotação
    Page<{Entidade}ResponseDTO> search(
        @Param("nome")      String nome,
        @Param("status")    String status,
        @Param("valorMin")  BigDecimal valorMin,
        @Param("valorMax")  BigDecimal valorMax,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim")   LocalDate dataFim,
        Pageable pageable
    );

    // 2. searchQuery — HQL com filtros dinâmicos (privado no repositório customizado via Criteria/StringBuilder)
    // 3. appendFilters — monta o WHERE dinamicamente
}
```

**Na implementação com QueryDSL ou Criteria (quando filtros são dinâmicos):**

```java
// Em implementação customizada do BaseCustomJpaRepository
private String buildSearchHql({Entidade}FilterParams filter) {
    StringBuilder hql = new StringBuilder("""
        SELECT new br.com.gems.{app}.dto.{Entidade}ResponseDTO(e.id, e.nome, e.status, e.dataCriacao)
        FROM {Entidade} e WHERE 1=1
        """);
    appendFilters(hql, filter);
    return hql.toString();
}

private void appendFilters(StringBuilder hql, {Entidade}FilterParams filter) {
    if (ObjectUtil.isNotNullOrEmpty(filter.getNome()))
        hql.append(" AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :nome, '%'))");
    if (ObjectUtil.isNotNullOrEmpty(filter.getStatus()))
        hql.append(" AND e.status = :status");
    if (!ObjectUtil.isNullOrEmpty(filter.getValorMin()))
        hql.append(" AND e.valor >= :valorMin");
    if (!ObjectUtil.isNullOrEmpty(filter.getValorMax()))
        hql.append(" AND e.valor <= :valorMax");
    if (!ObjectUtil.isNullOrEmpty(filter.getDataInicio()))
        hql.append(" AND e.dataCriacao >= :dataInicio");
    if (!ObjectUtil.isNullOrEmpty(filter.getDataFim()))
        hql.append(" AND e.dataCriacao <= :dataFim");
}
```

## Passo 4: Service

```java
// {Entidade}Service.java
public Page<{Entidade}ResponseDTO> search({Entidade}FilterParams filter, Pageable pageable) {
    return repository.search(
        filter.getNome(),
        filter.getStatus(),
        filter.getValorMin(),
        filter.getValorMax(),
        filter.getDataInicio(),
        filter.getDataFim(),
        pageable
    );
}
```

## Passo 5: Controller — POST /search

```java
// {Entidade}Controller.java
@PostMapping("/search")
public ResponseEntity<ApiResponseDTO<Page<{Entidade}ResponseDTO>>> search(
        @RequestBody {Entidade}FilterParams filter,
        @PageableDefault(size = 10, sort = "nome") Pageable pageable) {
    return ResponseEntity.ok(ApiResponseDTO.of(service.search(filter, pageable)));
}
```

- Método `POST` para enviar o filtro no body (suporta filtros complexos/nested).
- `@PageableDefault` define tamanho padrão e ordenação inicial.
- O `gems-rest-common` embala o retorno em `ApiResponseDTO` automaticamente.

## Passo 6: Store Angular

```typescript
// {entidade}.store.ts
search(filter: {Entidade}FilterModel, pageable: Pageable): Observable<{ content: {Entidade}ResponseModel[]; totalElements: number }> {
  return this.post<{ content: {Entidade}ResponseModel[]; totalElements: number }, {Entidade}FilterModel>(
    '/search',
    filter,
    { pageable }
  );
}
```

## Passo 7: Componente de Listagem

Ver template completo em [lists.md](../rules/frontend-angular/lists.md).

Pontos críticos:
1. `isLoading = signal(false)` — controla o skeleton.
2. `@if (isLoading()) { spinner } @else { gems-table }` — **nunca** `*ngIf`.
3. `onPageChange(newPageable)` — atualiza `this.pageable` e chama `loadEntidades()`.
4. `search()` — reseta `pageable.page = 0` antes de pesquisar.

```html
<!-- Skeleton de loading obrigatório antes da tabela -->
@if (isLoading()) {
  <div class="loading-state">
    <i class="fa-solid fa-circle-notch fa-spin"></i> Carregando {entidades}...
  </div>
} @else {
  <gems-table
    [columns]="columns"
    [data]="entidades()"
    [actions]="actions"
    [totalRecords]="totalRecords()"
    [page]="pageable.page ?? 0"
    [size]="pageable.size ?? 10"
    [sortField]="currentSortField"
    [sortDirection]="currentSortDirection"
    emptyMessage="Nenhum(a) {entidade} encontrado(a)."
    (actionClick)="handleAction($event)"
    (pageChange)="onPageChange($event)">
  </gems-table>
}
```

---

## Checklist de Conformidade

- [ ] `FilterParams` sem campos desnecessários (sem entidade completa).
- [ ] `ResponseDTO` com apenas os campos exibidos na tabela.
- [ ] Repository com trinca: `search()` + `countQuery` + `appendFilters()`.
- [ ] `WHERE 1=1` como âncora dos filtros dinâmicos.
- [ ] `ObjectUtil.isNotNullOrEmpty()` em cada filtro — nunca `!= null`.
- [ ] Controller usa `POST /search` (não `GET`) com `@RequestBody`.
- [ ] Store tipada com generics (`Observable<{ content: T[]; totalElements: number }>`).
- [ ] `isLoading` é um `signal<boolean>`.
- [ ] Template usa `@if` (não `*ngIf`).
- [ ] `onPageChange` atualiza `pageable` e rechama `loadEntidades()`.
- [ ] `search()` reseta `pageable.page = 0`.
