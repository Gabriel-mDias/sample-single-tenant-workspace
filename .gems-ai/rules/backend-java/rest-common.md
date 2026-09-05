---
stack: backend-java
module: gems-rest-common
severity: mandatory
see-also: [controllers.md, observability.md, validation.md]
---

# REST Common — gems-rest-common

> **TL;DR:**
> - `gems-rest-common` fornece `ApiResponseDTO` (envelope padrão), `PageResponseDTO` (paginação) e filtro de Correlation-ID.
> - Correlation-ID: gerado no frontend (interceptor HTTP), propagado como header `X-Correlation-Id`, armazenado no MDC do backend.
> - `ApiResponseDTO` é construído automaticamente pelo advice — **nunca** construa manualmente no Controller.
> - `Page<T>` é serializado automaticamente via Jackson — retorne `ResponseEntity<Page<T>>` no Controller.

## 1. Envelope de Resposta — ApiResponseDTO

O módulo `gems-rest-common` fornece uma estrutura de envelope padrão para respostas de erro gerenciadas pela `@RestControllerAdvice` do `gems-exception`.

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "correlationId": "abc-123-xyz",
  "errors": ["O nome é obrigatório.", "O e-mail é inválido!"]
}
```

O envelope é gerado automaticamente pelo advice da `gems-exception` — você **não** constrói `ApiResponseDTO` manualmente nos Controllers. Apenas lance `BusinessException(errors)` na Service e o advice cuida do envelope.

---

## 2. Paginação — PageResponseDTO

Ao retornar listas paginadas, o `Page<T>` do Spring é serializado pela SDK. O frontend espera o formato:

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "page": 0,
  "size": 10
}
```

Nos Controllers, retorne diretamente `ResponseEntity<Page<ResponseDTO>>` — a serialização acontece automaticamente via Jackson + configuração do módulo.

---

## 3. Correlation ID

O Correlation ID rastreia uma requisição ponta-a-ponta (frontend → backend → logs).

### Backend (gems-rest-common)
O módulo registra um filtro que:
1. Lê o header `X-Correlation-Id` da requisição.
2. Se ausente, gera um UUID novo.
3. Armazena no `MDC` do Log4j/Logback para aparecer em todos os logs daquela thread.
4. Retorna o valor no header de resposta `X-Correlation-Id`.

### Configuração (application.yml)
```yaml
gems:
  rest-common:
    correlation-id:
      enabled: true
      header-name: X-Correlation-Id
```

### Como usar em logs
Com o MDC configurado, o `correlationId` aparece automaticamente em cada linha de log se o padrão do appender incluir `%X{correlationId}`:
```
2024-01-15 10:30:00 [abc-123-xyz] INFO  Service - Iniciando insert de Entidade
```

Para a receita ponta-a-ponta de propagação front→back, veja `recipes/correlation-id-end-to-end.md`.

---

## 4. Configuração do Módulo

No `pom.xml` do módulo `{app}-core`:
```xml
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-rest-common</artifactId>
</dependency>
```

No BOM (`gems-bom`) a versão já está alinhada — não declare `<version>` manualmente.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `return ResponseEntity.ok(new ApiResponseDTO<>(true, data, null))` no Controller | Retornar `data` diretamente — o advice envelopa automaticamente |
| Gerar `X-Correlation-Id` no backend quando ausente sem tentar ler o header | O filtro lê o header; se ausente, gera um novo UUID |
| `Page<T>` serializado manualmente para JSON | Retornar `ResponseEntity<Page<T>>` — Jackson + configuração do módulo faz a serialização |
