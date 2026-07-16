package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.identity.dto.*;
import com.becommerce.crm.application.identity.port.input.AuthUseCase;
import com.becommerce.crm.application.identity.port.output.*;
import com.becommerce.crm.domain.identity.*;
import com.becommerce.crm.domain.identity.event.*;
import com.becommerce.crm.domain.identity.exception.*;
import com.becommerce.crm.domain.identity.valueobject.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EventPublisher eventPublisher;

    private static final int REFRESH_TOKEN_EXPIRY_DAYS = 7;
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider, EventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword().value())) {
            throw new InvalidCredentialsException();
        }

        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        List<String> roles = List.of("USER");
        List<String> permissions = List.of("read", "write");

        String accessToken = jwtProvider.generateAccessToken(user.getId(), user.getCompanyId(), roles, permissions);
        String family = UUID.randomUUID().toString();
        String refreshToken = jwtProvider.generateRefreshToken(user.getId(), family);

        refreshTokenRepository.save(RefreshToken.create(user.getId(), refreshToken, family, REFRESH_TOKEN_EXPIRY_DAYS));

        eventPublisher.publish(UserLoggedInEvent.create(user.getId(), user.getCompanyId(), "unknown"));

        return new LoginResponse(accessToken, refreshToken, user.getId().toString(), user.getEmail().value(), user.getName());
    }

    @Override
    public LoginResponse refreshTokens(RefreshTokenRequest request) {
        if (!jwtProvider.validateToken(request.refreshToken())) {
            throw new TokenExpiredException();
        }

        UUID userId = jwtProvider.extractUserId(request.refreshToken());
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        RefreshToken oldToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new TokenExpiredException("Refresh token not found"));

        if (oldToken.isRevoked() || oldToken.isExpired()) {
            throw new TokenExpiredException("Refresh token has been revoked or expired");
        }

        refreshTokenRepository.revokeByToken(request.refreshToken());

        List<String> roles = List.of("USER");
        List<String> permissions = List.of("read", "write");

        String newAccessToken = jwtProvider.generateAccessToken(user.getId(), user.getCompanyId(), roles, permissions);
        String newFamily = UUID.randomUUID().toString();
        String newRefreshToken = jwtProvider.generateRefreshToken(user.getId(), newFamily);

        refreshTokenRepository.save(RefreshToken.create(user.getId(), newRefreshToken, newFamily, REFRESH_TOKEN_EXPIRY_DAYS));

        eventPublisher.publish(TokenRefreshedEvent.create(user.getId(), user.getCompanyId()));

        return new LoginResponse(newAccessToken, newRefreshToken, user.getId().toString(), user.getEmail().value(), user.getName());
    }

    @Override
    public void logout(UUID userId, String refreshToken) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);
        refreshTokenRepository.revokeByToken(refreshToken);
        eventPublisher.publish(UserLoggedOutEvent.create(userId, user.getCompanyId()));
    }

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Email email = new Email(request.email());
        Password password = new Password(request.password());

        User user = User.create(email, password, request.name(), request.companyId());
        userRepository.save(user);

        eventPublisher.publish(UserCreatedEvent.create(user.getId(), request.email(), request.companyId()));
    }

    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            eventPublisher.publish(PasswordResetRequestedEvent.create(user.getId(), user.getCompanyId()));
        });
    }

    @Override
    public void resetPassword(String token, String newPassword) {
    }

    @Override
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(oldPassword, user.getPassword().value())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        Password newPwd = new Password(newPassword);
        user.updatePassword(newPwd);
        userRepository.save(user);

        eventPublisher.publish(PasswordChangedEvent.create(userId, user.getCompanyId()));
    }
}
