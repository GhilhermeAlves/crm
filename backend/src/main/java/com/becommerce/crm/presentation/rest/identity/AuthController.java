package com.becommerce.crm.presentation.rest.identity;

import com.becommerce.crm.application.identity.dto.*;
import com.becommerce.crm.application.identity.port.input.AuthUseCase;
import com.becommerce.crm.application.identity.port.input.UserUseCase;
import com.becommerce.crm.infrastructure.security.filter.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final UserUseCase userUseCase;

    public AuthController(AuthUseCase authUseCase, UserUseCase userUseCase) {
        this.authUseCase = authUseCase;
        this.userUseCase = userUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authUseCase.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authUseCase.forgotPassword(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authUseCase.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CurrentUser principal,
                                               @RequestBody ChangePasswordRequest request) {
        authUseCase.changePassword(principal.userId(), request.oldPassword(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CurrentUser principal) {
        UserResponse response = userUseCase.getUserById(principal.userId());
        return ResponseEntity.ok(UserResponse.withCurrentUser(
                response, principal.roles(), principal.membershipRole(), principal.permissions()));
    }
}
