package br.com.gems.app.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduz duas falhas de <b>requisição</b> que o {@code GlobalExceptionHandler} da SDK devolvia como
 * <b>500</b>: endereço inexistente e parâmetro de rota com tipo errado.
 *
 * <h2>Por que existe</h2>
 * O handler da SDK trata {@code Exception} de forma ampla e mapeia tudo que não reconhece para 500
 * {@code ERRO_NAO_ESPERADO}. Ele reconhece {@code BusinessException}, {@code SecurityException} e
 * {@code IllegalArgumentException} — e mais nada. Duas consequências, ambas medidas:
 *
 * <ul>
 *   <li><b>Qualquer endereço não mapeado respondia 500.</b> Um caminho digitado errado ficava
 *       indistinguível de aplicação quebrada em qualquer monitoração por taxa de 5xx — e, com a
 *       documentação OpenAPI desligada em produção, {@code /v3/api-docs} passou a ser um
 *       desses endereços.</li>
 *   <li><b>UUID malformado na rota respondia 500</b> nos endpoints que declaram
 *       {@code @PathVariable UUID} — os controllers que recebem id na rota. O Spring lança
 *       {@link MethodArgumentTypeMismatchException}, que não descende de
 *       {@code IllegalArgumentException} e por isso escapava. Os demais controllers do projeto
 *       recebem {@code String} e convertem no service, o que produz
 *       {@code IllegalArgumentException} e, aí sim, 400.</li>
 * </ul>
 *
 * <p>Esta classe fecha a divergência pelo lado que vale para o projeto inteiro, inclusive para
 * controllers que ainda não existem — em vez de depender de cada domínio lembrar de receber o id
 * como {@code String}.
 *
 * <h2>Sobre não vazar informação</h2>
 * O 404 não repete o caminho pedido no corpo da mensagem e o 400 não ecoa o valor recebido: os dois
 * dados já estão no {@code path} e no log, e devolvê-los no texto transformaria a resposta de erro
 * num espelho de entrada controlada por quem chama.
 *
 * <p>{@link Ordered#HIGHEST_PRECEDENCE} garante precedência sobre o handler genérico da SDK, como
 * em {@link AccessDeniedExceptionHandler} e {@link IdentityProviderExceptionHandler}.
 */
@Slf4j
@Order( Ordered.HIGHEST_PRECEDENCE )
@RestControllerAdvice
public class RequisicaoInvalidaExceptionHandler {

    @ExceptionHandler( NoResourceFoundException.class )
    public ResponseEntity<Map<String, Object>> handleNaoEncontrado( NoResourceFoundException ex,
                                                                   WebRequest request ) {
        var body = corpo( "RECURSO_NAO_ENCONTRADO", "O endereço solicitado não existe nesta API.", request );

        if ( request instanceof ServletWebRequest servletRequest ) {
            log.debug( "Endereço não mapeado: {} {}", servletRequest.getRequest().getMethod(),
                    servletRequest.getRequest().getRequestURI() );
        }

        return ResponseEntity.status( HttpStatus.NOT_FOUND ).body( body );
    }

    /**
     * Parâmetro de rota ou de consulta com tipo incompatível — na prática, quase sempre um
     * identificador que não é um UUID válido.
     *
     * <p>A mensagem nomeia o <b>parâmetro</b>, não o valor: quem chamou já sabe o que enviou, e
     * saber qual dos parâmetros está errado é o que resolve.
     */
    @ExceptionHandler( MethodArgumentTypeMismatchException.class )
    public ResponseEntity<Map<String, Object>> handleTipoInvalido( MethodArgumentTypeMismatchException ex,
                                                                   WebRequest request ) {
        var mensagem = "O parâmetro '" + ex.getName() + "' foi informado em um formato inválido.";
        var body = corpo( "PARAMETRO_INVALIDO", mensagem, request );

        if ( request instanceof ServletWebRequest servletRequest ) {
            log.warn( "Parâmetro '{}' em formato inválido em {} {}", ex.getName(),
                    servletRequest.getRequest().getMethod(), servletRequest.getRequest().getRequestURI() );
        }

        return ResponseEntity.status( HttpStatus.BAD_REQUEST ).body( body );
    }

    /** Espelha o formato de erro da SDK (occurrenceTime / errorType / message / path / method). */
    private Map<String, Object> corpo( String errorType, String message, WebRequest request ) {
        var body = new LinkedHashMap<String, Object>();
        body.put( "occurrenceTime", LocalDateTime.now() );
        body.put( "errorType", errorType );
        body.put( "message", message );

        if ( request instanceof ServletWebRequest servletRequest ) {
            body.put( "path", servletRequest.getRequest().getRequestURI() );
            body.put( "method", servletRequest.getRequest().getMethod() );
        }

        return body;
    }

}
