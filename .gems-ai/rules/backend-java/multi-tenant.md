---
stack: backend-java
module: gems-jpa-multi-tenant
severity: not-applicable
see-also: [security.md, persistence-db.md]
---

> ⛔ **NÃO SE APLICA A ESTE PROJETO.** Este é um sistema **single-tenant**: o schema é fixo
> (`public`, `security`, `auditoria`, Princípio II) e `gems-jpa-multi-tenant` não está no reator.
> Este arquivo fica como referência do módulo da SDK caso um projeto derivado precise dele — não
> introduza `JpaTenantContext`, filtro de tenant ou troca de schema aqui. Se o projeto vier a
> precisar de multi-tenancy, isso é emenda ao Princípio II, com bump e Sync Impact Report.

# Multi-Tenancy — gems-jpa-multi-tenant

> **TL;DR:**
> - Schema-based multi-tenancy via Hibernate connection providers.
> - `JpaTenantContext.setCurrentTenant(tenant)`: popule em um `RequestFilter` **antes** de qualquer acesso ao banco.
> - Cada tenant tem seu próprio schema no PostgreSQL — isolamento completo de dados.
> - **Obrigatório:** `JpaTenantContext.clear()` no bloco `finally` para evitar vazamento entre requisições.

## 1. Conceito

O `gems-jpa-multi-tenant` implementa multi-tenancy baseado em **schemas PostgreSQL**. Cada tenant tem seu próprio schema e o Hibernate troca automaticamente de schema em cada requisição com base no contexto.

```
Requisição HTTP
   │
   ▼
RequestFilter
   └── JpaTenantContext.setCurrentTenant("tenant-abc")
          │
          ▼
      Hibernate usa schema "tenant-abc" para todas as queries desta thread
```

---

## 2. Populando o Contexto do Tenant

Crie um `Filter` ou `HandlerInterceptor` que popule o contexto antes de qualquer acesso ao banco:

```java
@Component
@Order(1)
public class TenantContextFilter implements Filter {

    @Override
    public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain )
            throws IOException, ServletException {

        var httpRequest = (HttpServletRequest) request;

        // Extraia o tenant do header, JWT, subdomínio ou parâmetro — depende da estratégia do projeto
        var tenantId = extractTenantId( httpRequest );

        JpaTenantContext.setCurrentTenant( tenantId );
        try {
            chain.doFilter( request, response );
        } finally {
            JpaTenantContext.clear();  // Limpa após a requisição — obrigatório
        }
    }

    private String extractTenantId( HttpServletRequest request ) {
        // Estratégias comuns:
        // 1. Header: request.getHeader("X-Tenant-Id")
        // 2. JWT claim: extrair do token no SecurityContext
        // 3. Subdomínio: request.getServerName().split("\\.")[0]
        return request.getHeader( "X-Tenant-Id" );
    }
}
```

---

## 3. Configuração do Módulo

```xml
<!-- pom.xml -->
<dependency>
    <groupId>br.com.gems</groupId>
    <artifactId>gems-jpa-multi-tenant</artifactId>
</dependency>
```

```yaml
# application.yml
gems:
  jpa:
    multi-tenant:
      enabled: true
      default-schema: public   # Schema padrão para dados compartilhados
```

---

## 4. Regras

- **Sempre limpe o contexto** no `finally` — evita vazamento entre requisições no pool de threads.
- O `JpaTenantContext.setCurrentTenant()` deve ser chamado **antes** de qualquer operação JPA na thread.
- Dados compartilhados entre tenants (tabelas de referência) devem ficar no schema `public` ou em um schema dedicado.
- Testes de integração precisam configurar o contexto do tenant explicitamente.
- Não armazene o `tenantId` em variáveis de instância — use apenas o `JpaTenantContext` (ThreadLocal).

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| Omitir `JpaTenantContext.clear()` no `finally` | Sempre limpar no `finally` — evita vazamento entre requisições no pool |
| Setar `JpaTenantContext` depois de qualquer operação JPA | Setar o contexto **antes** de qualquer operação JPA na thread |
| `private String tenantId` como campo de instância | Usar apenas `JpaTenantContext` (ThreadLocal por requisição) |
