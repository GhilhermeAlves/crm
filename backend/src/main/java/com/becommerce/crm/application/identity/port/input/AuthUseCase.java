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
}