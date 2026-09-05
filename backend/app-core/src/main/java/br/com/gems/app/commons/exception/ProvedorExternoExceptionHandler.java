package br.com.gems.app.commons.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz {@link ProvedorExternoException} em <b>502 Bad Gateway</b>.
 *
 * <p>O {@code GlobalExceptionHandler} da SDK mapeia qualquer {@link Exception} não tratada para
 * <b>500</b>, o que faria uma queda do ViaCEP se apresentar como defeito do sistema. 502 é o que de
 * fato aconteceu: uma dependência a montante não respondeu.
 *
 * <p>Mesmo raciocínio e mesmo formato de corpo do
 * {@code IdentityProviderExceptionHandler}, que já faz isso para o provedor de identidade — este é o
 * segundo integrante da mesma solicitação, e o primeiro fora do Keycloak.
 */
@Slf4j
@Order( Ordered.HIGHEST_PRECEDENCE )
@RestControllerAdvice
public class ProvedorExternoExceptionHandler {

    @ExceptionHandler( ProvedorExternoException.class )
    public ResponseEntity<Map<String, Object>> handleProvedorExterno( ProvedorExternoException ex,
                                                                      WebRequest request ) {
        var body = new LinkedHashMap<String, Object>();
        body.put( "occurrenceTime", LocalDateTime.now() );
        body.put( "errorType", "FALHA" );
        body.put( "message", ex.getMessage() );

        if ( request instanceof ServletWebRequest servletRequest ) {
            body.put( "path", servletRequest.getRequest().getRequestURI() );
            body.put( "method", servletRequest.getRequest().getMethod() );
        }

        log.error( "Falha de provedor externo em {}", body.get( "path" ), ex );

        return ResponseEntity.status( HttpStatus.BAD_GATEWAY ).body( body );
    }

}
