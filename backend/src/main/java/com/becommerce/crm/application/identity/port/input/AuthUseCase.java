package com.becommerce.crm.application.identity.port.input;

import com.becommerce.crm.application.identity.dto.LoginRequest;
import com.becommerce.crm.application.identity.dto.LoginResponse;
import com.becommerce.crm.application.identity.dto.RefreshTokenRequest;
import com.becommerce.crm.application.identity.dto.RegisterRequest;
import java.util.UUID;

public interface AuthUseCase {
    LoginResponse login(LoginRequest request);
    LoginResponse refreshTokens(RefreshTokenRequest request);
    void logout(UUID userId, String refreshToken);
    void register(RegisterRequest request);
    void forgotPassword(String email);
    void resetPassword(String token, String newPassword);
    void changePassword(UUID userId, String oldPassword, String newPassword);
    LoginResponse handleKeycloakLogin(UUID userId);
}
