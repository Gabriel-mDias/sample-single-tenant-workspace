package br.com.gems.app.commons.exception;

import br.com.gems.exception.exception.dto.ExceptionResponseDTO;
import br.com.gems.exception.exception.enums.ErrorTypeEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/** Conversão inválida do JSON é erro de entrada, inclusive para UUID e enum dos filtros. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorpoInvalidoExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ExceptionResponseDTO tratar( HttpMessageNotReadableException exception, HttpServletRequest request ) {
        return ExceptionResponseDTO.builder()
                .occurrenceTime( LocalDateTime.now() )
                .errorType( ErrorTypeEnum.VALIDACAO )
                .codigo( "CORPO_INVALIDO" )
                .message( "O corpo da requisição está em um formato inválido." )
                .path( request.getRequestURI() )
                .method( request.getMethod() )
                .build();
    }
}
