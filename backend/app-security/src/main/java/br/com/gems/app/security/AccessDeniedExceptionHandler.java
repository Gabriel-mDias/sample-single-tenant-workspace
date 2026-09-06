package br.com.gems.app.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz negação de autorização do method security em <b>403 Forbidden</b>.
 *
 * <p>Sem isto, a {@link AuthorizationDeniedException} lançada pelo {@code @PreAuthorize}
 * não é reconhecida pelo {@code GlobalExceptionHandler} da SDK e escapa como <b>500</b> —
 * por exemplo, um usuário sem a ação tentando {@code DELETE /api/parceiros/{id}}
 * (endpoint restrito a {@code ADMIN}) recebia 500 em vez de 403.
 *
 * <p>{@link Ordered#HIGHEST_PRECEDENCE} garante precedência sobre o handler genérico da SDK.
 * O corpo espelha o formato de erro da SDK (occurrenceTime / errorType / message / path / method).
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AccessDeniedExceptionHandler {

    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public ResponseEntity<Map<String, Object>> handleAccessDenied( RuntimeException ex, WebRequest request ) {
        var body = new LinkedHashMap<String, Object>();
        body.put( "occurrenceTime", LocalDateTime.now() );
        body.put( "errorType", "ACESSO_NEGADO" );
        body.put( "message", "Você não tem permissão para executar esta ação." );

        if ( request instanceof ServletWebRequest servletRequest ) {
            body.put( "path", servletRequest.getRequest().getRequestURI() );
            body.put( "method", servletRequest.getRequest().getMethod() );
            log.warn( "Acesso negado em {} {}", servletRequest.getRequest().getMethod(),
                    servletRequest.getRequest().getRequestURI() );
        }

        return ResponseEntity.status( HttpStatus.FORBIDDEN ).body( body );
    }

}
