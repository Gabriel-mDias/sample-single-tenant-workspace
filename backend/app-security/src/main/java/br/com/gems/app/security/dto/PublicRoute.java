package br.com.gems.app.security.dto;

import org.springframework.http.HttpMethod;

public record PublicRoute(HttpMethod method, String pattern) {
}
