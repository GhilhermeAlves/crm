package com.becommerce.auth.application.identity.port.output;

/**
 * Port for authenticating against Keycloak with email/password credentials.
 * Implementação: KeycloakAdminClient (via Keycloak Admin REST API).
 */
public interface KeycloakAuthPort {
    /**
     * Authenticate user with email and password directly against Keycloak.
     * @param email User email
     * @param password User password
     * @return JWT token from Keycloak
     * @throws InvalidCredentialsException if email/password invalid
     * @throws KeycloakUnavailableException if Keycloak unreachable
     */
    String authenticateWithCredentials(String email, String password);
}
