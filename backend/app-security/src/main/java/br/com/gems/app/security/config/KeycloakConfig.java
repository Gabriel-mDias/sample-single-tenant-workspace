package br.com.gems.app.security.config;

import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AppKeycloakProperties.class)
public class KeycloakConfig {

    private final AppKeycloakProperties keycloakProperties;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl( keycloakProperties.getServerUrl() )
                .realm( keycloakProperties.getRealm() )
                .grantType( OAuth2Constants.CLIENT_CREDENTIALS )
                .clientId( keycloakProperties.getClientId() )
                .clientSecret( keycloakProperties.getClientSecret() )
                .build();
    }
}
