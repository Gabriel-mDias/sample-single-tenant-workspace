---
stack: backend-java
module: gems-aws, gems-aws-web
severity: mandatory
see-also: [services.md, validation.md]
---

# Storage & Upload S3 — gems-aws / gems-aws-web

> **TL;DR:**
> - Uploads via Presigned URLs — o backend **NÃO** recebe bytes do arquivo diretamente.
> - Fluxo: Frontend → `POST /generate-upload-url` → `{ url, fileKey }` → PUT binário no S3 → DTO com `fileKey`.
> - **Obrigatório:** `s3Service.fileExists(fileKey)` na Service **antes** de persistir qualquer `fileKey` no banco.
> - `gems-aws-web` fornece o `S3Controller` pronto — **nunca** re-implemente o endpoint de geração de URL.

## 1. Fluxo de Upload (Presigned URL)

O backend atua **somente** como despachante de credenciais. O arquivo pesado vai direto do browser para o S3.

```
Frontend                Backend                 AWS S3
   │                       │                      │
   │──POST /generate-url──▶│                      │
   │◀──{ url, fileKey }────│                      │
   │                       │                      │
   │──────────────────PUT binary───────────────▶  │
   │                       │                      │
   │──POST /api/{recurso} { fileKey }──▶           │
   │          service.fileExists(fileKey) ──▶      │
   │          repository.save(entity) ──▶          │
```

### Etapas:
1. Frontend solicita URL em `POST /api/aws/s3/generate-upload-url` (do `S3Controller` do `gems-aws-web`).
2. Backend retorna `PresignedUrlResponseDTO` com `url` temporária e `fileKey`.
3. Frontend faz `PUT` binário diretamente na URL (não passa pelo backend — evita gargalos).
4. Frontend inclui o `fileKey` no DTO de submit da entidade.
5. Backend **valida** `s3Service.fileExists(fileKey)` antes de persistir.

---

## 2. Endpoint de Geração de URL (já fornecido pelo gems-aws-web)

```java
// Já existe no S3Controller — reutilize, não re-implemente
POST /api/aws/s3/generate-upload-url
Body: { "fileName": "documento.pdf", "contentType": "application/pdf" }
Response: { "url": "https://s3.aws.../...", "fileKey": "uploads/uuid-filename.pdf" }
```

---

## 3. Validação na Service (Obrigatória)

```java
private void validateUploads( List<DocumentoUploadDTO> uploads ) {
    var errors = new ArrayList<String>();

    for ( var upload : uploads ) {
        if ( ObjectUtil.isNotNullAndNotEmpty( upload.getFileKey() ) ) {
            if ( !s3Service.fileExists( upload.getFileKey() ) ) {
                errors.add( "O arquivo informado não existe no S3: " + upload.getFileKey() );
            }
        }
    }

    if ( !errors.isEmpty() ) {
        throw new BusinessException( errors );
    }
}
```

Chame `validateUploads()` **dentro do `validate(dto)`** principal antes de qualquer persistência.

---

## 4. Downloads e Deleção

```java
// Download — URL temporária (expira rápido, sem compartilhamento público)
GET /api/aws/s3/generate-download-url?fileKey=uploads/uuid-filename.pdf

// Deleção física — só quando a regra de negócio exigir
s3Service.deleteFile( fileKey );
```

Se a entidade for deletada (soft delete), **mantenha** o arquivo no S3 por padrão. Só delete o arquivo fisicamente se a regra de negócio exigir deleção permanente.

---

## 5. Configuração do Módulo

```xml
<!-- pom.xml — use gems-aws-web para ter o S3Controller embutido -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-aws-web</artifactId>
</dependency>
```

```yaml
# application.yml
gems:
  aws:
    bucket: nome-do-bucket
    region: us-east-1
    presigned-url-expiration: 300   # segundos
```

Para a receita completa ponta-a-ponta (incluindo o upload Angular), veja `recipes/file-upload-s3-end-to-end.md`.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Receber bytes do arquivo em `@PostMapping` (multipart) | Presigned URL — arquivo vai direto do browser para o S3 |
| Persistir `fileKey` sem chamar `s3Service.fileExists(fileKey)` | Sempre validar existência no S3 **antes** de persistir |
| Criar controller próprio `POST /upload-url` | Reutilizar o endpoint já fornecido por `gems-aws-web` |
| Deletar arquivo do S3 quando entidade é soft-deleted | Manter no S3 por padrão — só deletar se regra de negócio exigir |
