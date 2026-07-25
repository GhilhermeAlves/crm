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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EventPublisher eventPublisher;
    private final EmailService emailService;

    private static final int REFRESH_TOKEN_EXPIRY_DAYS = 7;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 60;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       RoleRepository roleRepository, UserRoleRepository userRoleRepository,
                       RolePermissionRepository rolePermissionRepository,
                       PermissionRepository permissionRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider, EventPublisher eventPublisher,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
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

        List<String> roles = userRoleRepository.findByUserIdAndCompanyId(user.getId(), user.getCompanyId()).stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        List<String> permissions = roles.stream()
                .flatMap(roleName -> {
                    try {
                        var role = roleRepository.findByNameAndCompanyId(
                                com.becommerce.crm.domain.identity.valueobject.RoleName.valueOf(roleName),
                                user.getCompanyId());
                        return role.map(r -> rolePermissionRepository.findByRoleId(r.getId()).stream()
                                .map(rp -> permissionRepository.findById(rp.getPermissionId()))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .map(Permission::getName))
                                .orElse(java.util.stream.Stream.empty());
                    } catch (Exception e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .distinct()
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            roles = List.of("USER");
        }
        if (permissions.isEmpty()) {
            permissions = List.of("dashboard:view");
        }

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

        List<String> roles = userRoleRepository.findByUserIdAndCompanyId(user.getId(), user.getCompanyId()).stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        List<String> permissions = roles.stream()
                .flatMap(roleName -> {
                    try {
                        var role = roleRepository.findByNameAndCompanyId(
                                com.becommerce.crm.domain.identity.valueobject.RoleName.valueOf(roleName),
                                user.getCompanyId());
                        return role.map(r -> rolePermissionRepository.findByRoleId(r.getId()).stream()
                                .map(rp -> permissionRepository.findById(rp.getPermissionId()))
                                .filter(java.util.Optional::isPresent)
                                .map(java.util.Optional::get)
                                .map(Permission::getName))
                                .orElse(java.util.stream.Stream.empty());
                    } catch (Exception e) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .distinct()
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            roles = List.of("USER");
        }
        if (permissions.isEmpty()) {
            permissions = List.of("dashboard:view");
        }

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

        User user = User.create(email, password, request.name(), "", request.companyId());
        userRepository.save(user);

        eventPublisher.publish(UserCreatedEvent.create(user.getId(), request.email(), request.companyId()));
    }

    @Override
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(token, user.getId(), RESET_TOKEN_EXPIRY_MINUTES);
            passwordResetTokenRepository.save(resetToken);
            eventPublisher.publish(PasswordResetRequestedEvent.create(user.getId(), user.getCompanyId(), email, token));
        });
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
            .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Reset token has expired or already been used");
        }

        User user = userRepository.findById(resetToken.getUserId())
            .orElseThrow(UserNotFoundException::new);

        Password password = new Password(newPassword);
        user.updatePassword(password);
        userRepository.save(user);

        resetToken.markAsUsed();
        passwordResetTokenRepository.save(resetToken);

        eventPublisher.publish(PasswordChangedEvent.create(user.getId(), user.getCompanyId()));
    }

    @Override
    public LoginResponse handleKeycloakLogin(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(UserNotFoundException::new);

        user.recordLogin();
        userRepository.save(user);

        eventPublisher.publish(UserLoggedInEvent.create(user.getId(), user.getCompanyId(), "keycloak"));

        return new LoginResponse(null, null, user.getId().toString(), user.getEmail().value(), user.getName());
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
