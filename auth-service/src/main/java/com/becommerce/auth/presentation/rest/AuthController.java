package com.becommerce.auth.presentation.rest;

import com.becommerce.auth.application.identity.dto.LoginRequest;
import com.becommerce.auth.application.identity.dto.LoginResponse;
import com.becommerce.auth.application.identity.service.CredentialsAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class AuthController {

    private final CredentialsAuthService credentialsAuthService;

    public AuthController(CredentialsAuthService credentialsAuthService) {
        this.credentialsAuthService = credentialsAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String jwtToken = credentialsAuthService.authenticateWithCredentials(
            request.email(),
            request.password()
        );

        LoginResponse response = LoginResponse.from(
            jwtToken,
            "Bearer",
            3600
        );

        return ResponseEntity.ok(response);
    }
}
