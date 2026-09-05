package br.com.gems.app.commons.exception;

/**
 * Falha de um provedor externo do qual o sistema depende, mas que não controla.
 *
 * <p>Existe para separar "o serviço de fora caiu" de "o sistema tem um defeito" e de "quem chamou
 * mandou algo errado". Sem ela, uma indisponibilidade do ViaCEP chegaria ao cliente como <b>500</b>,
 * indistinguível de um bug do sistema — e a monitoração acusaria erro da aplicação onde não há.
 *
 * <p>Traduzida para <b>502 Bad Gateway</b> por {@link ProvedorExternoExceptionHandler}, seguindo o
 * mesmo precedente já adotado para o provedor de identidade.
 */
public class ProvedorExternoException extends RuntimeException {

    public ProvedorExternoException( String message, Throwable cause ) {
        super( message, cause );
    }

}
