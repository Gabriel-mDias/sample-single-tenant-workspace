# Recipe: Erros e Validação Ponta-a-Ponta

**Objetivo:** Implementar o ciclo completo de validação e tratamento de erros, desde a `BusinessException` no backend até a exibição via `AlertService.errorFromApi()` e `gems-field-error` no frontend.

**Pré-requisitos:**
- `gems-exception` (BusinessException, ExceptionAdvice)
- `gems-validation` (constraints BR)
- `gems-rest-common` (ApiResponseDTO)
- `gems-sdk` Angular (AlertService)

**Rules relacionadas:**
- [validation.md](../rules/backend-java/validation.md)
- [services.md](../rules/backend-java/services.md)
- [rest-common.md](../rules/backend-java/rest-common.md)
- [feedback-alerts.md](../rules/frontend-angular/feedback-alerts.md)
- [gems-components.md](../rules/frontend-angular/gems-components.md)

---

## Camada 1: Bean Validation (Controller)

```java
// {Entidade}DTO.java
@Data
public class {Entidade}DTO {
    @NotBlank(message = "Nome é obrigatório.")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
    private String nome;

    @ValidEmail(message = "E-mail inválido.")         // gems-validation
    private String email;

    @ValidCpf(message = "CPF inválido.")              // gems-validation
    private String cpf;

    @ValidCnpj(message = "CNPJ inválido.")            // gems-validation
    private String cnpj;
}
```

```java
// Controller — @Valid aciona Bean Validation
@PostMapping
public ResponseEntity<ApiResponseDTO<{Entidade}DTO>> insert(@Valid @RequestBody {Entidade}DTO dto) {
    setBodyToExceptionLog(dto);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponseDTO.of(modelMapper.map(service.insert(dto), {Entidade}DTO.class)));
}
```

O `ExceptionAdvice` do `gems-exception` intercepta `MethodArgumentNotValidException` e retorna:
```json
{
  "success": false,
  "errors": ["Nome é obrigatório.", "E-mail inválido."]
}
```

## Camada 2: Business Validation (Service)

Validações de negócio que dependem de estado do banco:

```java
private void validate({Entidade}DTO dto) {
    List<String> errors = new ArrayList<>();

    if (ObjectUtil.isNullOrEmpty(dto.getNome()))
        errors.add("Nome é obrigatório.");

    if (ObjectUtil.isNotNullOrEmpty(dto.getEmail()) &&
        repository.existsByEmailAndIdNot(dto.getEmail(), dto.getId() != null ? UUID.fromString(dto.getId()) : null))
        errors.add("E-mail já cadastrado para outro registro.");

    if (!errors.isEmpty())
        throw new BusinessException(errors);   // Lança com lista de erros
}
```

`BusinessException` pode receber:
- `new BusinessException("Mensagem única.")` — string
- `new BusinessException(errors)` — lista de strings

## Camada 3: Formato da Resposta de Erro (ApiResponseDTO)

O `gems-rest-common` + `gems-exception` garantem que **todos** os erros (Bean Validation, BusinessException, 401, 403, 404, 500) retornam no mesmo formato:

```json
{
  "success": false,
  "data": null,
  "errors": [
    "Nome é obrigatório.",
    "E-mail já cadastrado para outro registro."
  ]
}
```

## Camada 4: Frontend — AlertService.errorFromApi()

```typescript
// Em qualquer subscribe de chamada HTTP:
this.store.create(dto).subscribe({
  next: (res) => {
    this.alertService.success('Sucesso', 'Criado com sucesso.');
    this.nav.navigate(['/{modulo}/list']);
  },
  error: (err) => {
    this.alertService.errorFromApi(err);   // Parse automático do ApiResponseDTO
    this.isLoading.set(false);
  }
});
```

`errorFromApi` exibe automaticamente todas as strings do array `errors` — uma por linha em um modal SweetAlert2.

## Camada 5: gems-field-error (Erros por Campo)

Para associar erros a campos específicos do formulário:

```html
<!-- No template do formulário -->
<div class="form-group">
  <label class="required fw-bold" for="email">E-mail</label>
  <input type="email" id="email" class="form-control"
         formControlName="email"
         [class.is-invalid]="form.get('email')?.invalid && form.get('email')?.touched">
  <gems-field-error [control]="form.get('email')">
  </gems-field-error>
</div>
```

`gems-field-error` exibe mensagens de erro do controle reativo quando o campo está `invalid` + `touched`.

## Camada 6: Validação no Step (Wizard)

Em steps do stepper, validação local antes de avançar:

```typescript
next(): void {
  const errors: string[] = [];
  if (!this.nome()) errors.push('Nome é obrigatório.');
  if (!this.email()) errors.push('E-mail é obrigatório.');

  if (errors.length > 0) {
    this.alertService.warning('Campos obrigatórios', errors.join('\n'));
    return;
  }
  this.stateService.updateState({ nome: this.nome(), email: this.email() });
  this.stateService.setStep(2);
}
```

---

## Checklist de Conformidade

- [ ] `@Valid` no parâmetro do controller ativa Bean Validation.
- [ ] `gems-validation` para CPF, CNPJ, e-mail (não reinventar regex).
- [ ] `validate()` no service acumula erros em `ArrayList<String>`.
- [ ] `ObjectUtil.isNullOrEmpty()` em cada verificação — nunca `== null`.
- [ ] `BusinessException(errors)` recebe a lista completa.
- [ ] Frontend usa `alertService.errorFromApi(err)` em todo `error:` de chamada HTTP.
- [ ] `gems-field-error` em campos com validação no template.
- [ ] `isLoading.set(false)` no bloco `error:` sempre.
