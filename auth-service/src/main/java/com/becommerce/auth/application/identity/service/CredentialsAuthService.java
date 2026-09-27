package com.becommerce.auth.application.identity.service;

import com.becommerce.auth.application.identity.port.output.KeycloakAuthPort;
import org.springframework.stereotype.Service;

@Service
public class CredentialsAuthService {

    private final KeycloakAuthPort keycloakAuthPort;

    public CredentialsAuthService(KeycloakAuthPort keycloakAuthPort) {
        this.keycloakAuthPort = keycloakAuthPort;
    }

    /**
     * Authenticate user with email and password.
     * Returns JWT token from Keycloak ready to use in subsequent API calls.
     */
    public String authenticateWithCredentials(String email, String password) {
        return keycloakAuthPort.authenticateWithCredentials(email, password);
    }
}
