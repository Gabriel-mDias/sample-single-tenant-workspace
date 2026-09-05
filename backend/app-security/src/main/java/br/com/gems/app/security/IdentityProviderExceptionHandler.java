package br.com.gems.app.security;

import br.com.gems.exception.exception.SecurityException;
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
 * Traduz falha de provisionamento no provedor de identidade em <b>502 Bad Gateway</b>.
 *
 * <p>O {@code GlobalExceptionHandler} da SDK mapeia {@link SecurityException} para <b>401</b>. Essa
 * escolha faz sentido quando a exceção representa credencial inválida de quem chamou — mas aqui
 * ela tem um único emissor: o {@code KeycloakUserService}, ao falhar em <b>criar ou alterar a conta
 * de outra pessoa</b> pela Admin API. Quem chamou está perfeitamente autenticado; quem recusou foi o
 * Keycloak.
 *
 * <p>O 401 mente sobre a causa. Para qualquer cliente, gateway ou painel de monitoração, um 401 numa
 * chamada autenticada é indistinguível de token expirado — a resposta padrão é renovar a sessão ou
 * mandar o usuário logar de novo, que aqui não conserta nada. A SPA de hoje ainda não tem
 * interceptor que deslogue nesse caso (o {@code errorFromApi} apenas exibe a mensagem), mas basta um
 * para que uma recusa do Keycloak passe a descartar o wizard inteiro. Foi com esse status que o
 * service account sem as roles de {@code realm-management} se apresentou: a Admin API respondia 403
 * e a API devolvia "não autenticado".
 *
 * <p>502 é o que de fato aconteceu: uma dependência a montante recusou a operação. A mensagem
 * original é preservada porque é ela que distingue "senha fora da política" de "service account sem
 * permissão" — as permissões administrativas do backend são concedidas declarativamente em
 * {@code environments/shared/keycloak/app-realm.yaml}.
 *
 * <p>{@link Ordered#HIGHEST_PRECEDENCE} garante precedência sobre o handler da SDK, como em
 * {@link AccessDeniedExceptionHandler}; o corpo espelha o mesmo formato de erro.
 */
@Slf4j
@Order( Ordered.HIGHEST_PRECEDENCE )
@RestControllerAdvice
public class IdentityProviderExceptionHandler {

    @ExceptionHandler( SecurityException.class )
    public ResponseEntity<Map<String, Object>> handleIdentityProviderFailure( SecurityException ex,
                                                                             WebRequest request ) {
        var body = new LinkedHashMap<String, Object>();
        body.put( "occurrenceTime", LocalDateTime.now() );
        body.put( "errorType", "FALHA" );
        body.put( "message", ex.getMessage() );

        if ( request instanceof ServletWebRequest servletRequest ) {
            body.put( "path", servletRequest.getRequest().getRequestURI() );
            body.put( "method", servletRequest.getRequest().getMethod() );
        }

        log.error( "Falha do provedor de identidade em {}", body.get( "path" ), ex );

        return ResponseEntity.status( HttpStatus.BAD_GATEWAY ).body( body );
    }

}
