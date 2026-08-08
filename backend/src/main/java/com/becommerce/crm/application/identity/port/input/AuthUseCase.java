package com.becommerce.crm.application.identity.port.input;

import com.becommerce.crm.application.identity.dto.RegisterRequest;
import com.becommerce.crm.domain.identity.User;
import java.util.UUID;

public interface AuthUseCase {
    void register(RegisterRequest request);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    void changePassword(UUID userId, String oldPassword, String newPassword);

    User provisionKeycloakUser(String keycloakSub, String email, String preferredUsername,
                               String givenName, String familyName);

    /**
     * Provisão ciente do provedor (Sprint 7.2). {@code provider} é o alias do
     * Identity Provider do Keycloak ({@code identity_provider} claim); {@code null}
     * indica login direto no realm. Para provedor externo (ex.: {@code google}),
     * um match por e-mail com conta local NUNCA auto-vincula
     * ({@link com.becommerce.crm.domain.identity.exception.LinkingRequiredException})
     * — exige fluxo explícito de verificação.
     */
    User provisionKeycloakUser(String keycloakSub, String email, String preferredUsername,
                               String givenName, String familyName, String provider);

    /**
     * Vincula (link) uma identidade externa à conta local encontrada por e-mail
     * (Caso B, Sprint 7.2), após verificar a senha da conta local. Idempotente:
     * se o {@code keycloakSub} já está vinculado, retorna a conta sem alterações.
     *
     * @throws com.becommerce.crm.domain.identity.exception.InvalidCredentialsException
     *         quando a senha não confere
     */
    User linkKeycloakIdentity(String keycloakSub, String email, String givenName, String familyName,
                              String rawPassword);
}