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
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/metrics").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakIdentityConverter)));
        return http.build();
    }
}
