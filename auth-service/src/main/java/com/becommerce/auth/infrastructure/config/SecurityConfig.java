package com.becommerce.auth.infrastructure.config;

import com.becommerce.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.becommerce.auth.infrastructure.security.KeycloakIdentityConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configura o crm-auth-service como resource server que valida o JWT do Keycloak
 * via JWKS do Keycloak. Nenhum endpoint emite token, e não há JWKS próprio.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final KeycloakIdentityConverter keycloakIdentityConverter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(KeycloakIdentityConverter keycloakIdentityConverter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.keycloakIdentityConverter = keycloakIdentityConverter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/health",
                                "/auth/health/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/metrics").permitAll()
                        // Access Gateway OIDC (Sprints 6.1/6.2): entrada de login,
                        // callback, logout e refresh — públicos por definição (a
                        // autenticação do refresh é feita via cookie de sessão +
                        // GatewayCsrfFilter; o logout via cookie + SameSite=Lax).
                        // /auth/providers (Sprint 7.0): catálogo público de provedores
                        // de identidade (aliases/labels/available) — sem segredos.
                        // /auth/link-status + /auth/link (Sprint 7.2): fluxo de
                        // vínculo de conta local — públicos por definição (autenticados
                        // via cookie crm_pending_link + CSRF cookie-to-header; o
                        // POST /auth/link nunca vaza tokens).
                        .requestMatchers(
                                "/auth/authorize",
                                "/auth/callback",
                                "/auth/logout",
                                "/auth/refresh",
                                "/auth/providers",
                                "/auth/link-status",
                                "/auth/link").permitAll()
                        // BFF relay (Sprint 6.4): autenticado via cookie de sessão
                        // (GatewayApiRelay) — nunca exige JWT do browser.
                        .requestMatchers("/api/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakIdentityConverter)));
        return http.build();
    }
}
