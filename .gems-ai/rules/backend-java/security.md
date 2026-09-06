---
stack: backend-java
module: gems-exception, spring-security
severity: mandatory
see-also: [rest-common.md, controllers.md, openapi.md]
---

# Segurança & Autenticação — Spring Security + gems-exception

> **TL;DR:**
> - Backend como OAuth2 Resource Server: valida JWT gerado pelo IdP (Keycloak ou similar) — não gerencia sessões.
> - `gems-exception` com `security.enabled: true` trata 401/403 e formata como `ApiResponseDTO`.
> - Roles via `@PreAuthorize("hasRole('ROLE_NOME')")` nos Controllers — requer `@EnableMethodSecurity`.
> - **NUNCA** armazene credenciais ou segredos no código — use variáveis de ambiente ou Vault.
> - Frontend usa PKCE + Authorization Code — backend apenas valida o token.

## 1. Configuração do Resource Server

O backend valida tokens JWT emitidos pelo IdP (ex.: Keycloak) — não faz login nem gerencia sessões:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain( HttpSecurity http ) throws Exception {
        http
            .csrf( AbstractHttpConfigurer::disable )
            .sessionManagement( session -> session.sessionCreationPolicy( SessionCreationPolicy.STATELESS ) )
            .authorizeHttpRequests( auth -> auth
                .requestMatchers( "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**" ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer( oauth2 -> oauth2.jwt( Customizer.withDefaults() ) );
        return http.build();
    }
}
```

```yaml
# application.yml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://{idp-host}/realms/{realm}
          jwk-set-uri: https://{idp-host}/realms/{realm}/protocol/openid-connect/certs
```

---

## 2. Autorização por Role

```java
// No Controller — proteção por endpoint
@PreAuthorize( "hasRole('ADMINISTRADOR')" )
@PostMapping
public ResponseEntity<{Entidade}DTO> insert( ... ) { ... }

// Proteção por método na Service (quando a lógica de autorização é mais complexa)
@PreAuthorize( "hasAnyRole('ADMINISTRADOR', 'SUPERVISOR')" )
public {Entidade}DTO insert( {Entidade}DTO dto ) { ... }
```

Roles devem estar no claim `realm_access.roles` ou `resource_access.{client}.roles` do JWT — conforme configuração do IdP.

---

## 3. Acessando o Token na Service

```java
@Service
@RequiredArgsConstructor
public class {Entidade}Service {

    // Obter o usuário autenticado atual
    private String getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if ( authentication instanceof JwtAuthenticationToken jwtAuth ) {
            return jwtAuth.getToken().getSubject();  // subject = userId no Keycloak
        }
        throw new BusinessException( "Usuário não autenticado!" );
    }

    // Verificar roles programaticamente (alternativa a @PreAuthorize)
    private boolean hasRole( String role ) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .anyMatch( a -> a.getAuthority().equals( "ROLE_" + role ) );
    }
}
```

---

## 4. Tratamento de Erros de Segurança (gems-exception)

Com `gems.exception.security.enabled: true`, o advice da `gems-exception` intercepta:

- `401 Unauthorized` → token ausente ou expirado → formata como `ApiResponseDTO`.
- `403 Forbidden` → role insuficiente → formata como `ApiResponseDTO`.
- Nenhum tratamento adicional necessário nos Controllers.

```yaml
gems:
  exception:
    security:
      enabled: true
```

---

## 5. Regras

- **Nunca** armazene `client_secret`, senhas ou chaves no código — use variáveis de ambiente.
- **Stateless**: sem `HttpSession`, sem cookies de sessão — apenas JWT.
- `@EnableMethodSecurity` é necessário para `@PreAuthorize` funcionar.
- Para a receita completa com frontend Angular (PKCE + guard de rota), veja `recipes/secured-resource-end-to-end.md`.
- Em testes de integração, use `@WithMockUser` ou configure um JWT de teste para simular roles.

---

## Anti-Patterns (NUNCA)

| Proibido | Correto |
| :--- | :--- |
| `client_secret` ou senha hardcoded no `application.yml` | Variáveis de ambiente ou Vault |
| `HttpSession` ou cookie de sessão | JWT stateless (`SessionCreationPolicy.STATELESS`) |
| `@PreAuthorize` sem `@EnableMethodSecurity` na `SecurityConfig` | Adicionar `@EnableMethodSecurity` — sem ela, `@PreAuthorize` é ignorado |
| Tratar erros 401/403 manualmente no Controller | `gems.exception.security.enabled: true` — advice cuida do envelope |
