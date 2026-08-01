package com.becommerce.crm.application.identity.service;

import com.becommerce.crm.application.company.port.output.CompanyRepository;
import com.becommerce.crm.application.identity.dto.RegisterRequest;
import com.becommerce.crm.application.identity.port.input.AuthUseCase;
import com.becommerce.crm.application.identity.port.output.EmailService;
import com.becommerce.crm.application.identity.port.output.EventPublisher;
import com.becommerce.crm.application.identity.port.output.PasswordEncoder;
import com.becommerce.crm.application.identity.port.output.PasswordResetTokenRepository;
import com.becommerce.crm.application.identity.port.output.RoleRepository;
import com.becommerce.crm.application.identity.port.output.UserRepository;
import com.becommerce.crm.application.identity.port.output.UserRoleRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.company.CompanyStatus;
import com.becommerce.crm.domain.identity.PasswordResetToken;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.event.PasswordChangedEvent;
import com.becommerce.crm.domain.identity.event.PasswordResetRequestedEvent;
import com.becommerce.crm.domain.identity.event.UserCreatedEvent;
import com.becommerce.crm.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.crm.domain.identity.exception.InvalidTokenException;
import com.becommerce.crm.domain.identity.exception.UserNotFoundException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService implements AuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final EmailService emailService;

    private static final int RESET_TOKEN_EXPIRY_MINUTES = 60;

    @Value("${app.auth.provisioning.enabled:true}")
    private boolean provisioningEnabled;

    @Value("${app.auth.provisioning.default-company-id:}")
    private String defaultCompanyId;

    @Value("${app.auth.provisioning.default-role:AGENT}")
    private String defaultRoleName;

    @Lazy
    @Autowired
    private AuthService self;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       RoleRepository roleRepository, UserRoleRepository userRoleRepository,
                       CompanyRepository companyRepository,
                       PasswordEncoder passwordEncoder, EventPublisher eventPublisher,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
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
    @Transactional
    public User provisionKeycloakUser(String keycloakSub, String email, String preferredUsername,
                                      String givenName, String familyName) {
        String resolvedEmail = resolveEmail(email, preferredUsername);

        if (!provisioningEnabled) {
            User existing = findExistingKeycloakUser(keycloakSub, resolvedEmail);
            if (existing != null) {
                rejectIfInactive(existing);
                return existing;
            }
            throw new UserProvisioningException(
                "Auto-provisioning de usuários do Keycloak está desabilitado.");
        }

        User existing = findExistingKeycloakUser(keycloakSub, resolvedEmail);
        if (existing != null) {
            boolean changed = syncKeycloakIdentity(existing, keycloakSub, resolvedEmail, givenName, familyName);
            User resolved = changed ? userRepository.save(existing) : existing;
            rejectIfInactive(resolved);
            return resolved;
        }

        if (keycloakSub == null || keycloakSub.isBlank()) {
            throw new UserProvisioningException(
                "Não foi possível provisionar o usuário: token sem subject (sub).");
        }
        if (resolvedEmail == null) {
            throw new UserProvisioningException(
                "Não foi possível provisionar o usuário: nenhum e-mail válido no token do Keycloak.");
        }

        try {
            return self.createProvisionedUser(keycloakSub, resolvedEmail, givenName, familyName);
        } catch (DataIntegrityViolationException e) {
            User raced = findExistingKeycloakUser(keycloakSub, resolvedEmail);
            if (raced != null) {
                boolean changed = syncKeycloakIdentity(raced, keycloakSub, resolvedEmail, givenName, familyName);
                return changed ? userRepository.save(raced) : raced;
            }
            throw new UserProvisioningException(
                "Não foi possível provisionar o usuário após conflito de criação: " + resolvedEmail);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createProvisionedUser(String keycloakSub, String email, String givenName, String familyName) {
        UUID companyId = resolveDefaultCompanyId();
        String firstName = givenName != null && !givenName.isBlank() ? givenName : email.substring(0, email.indexOf('@'));
        String lastName = familyName != null && !familyName.isBlank() ? familyName : "";

        User user = User.create(new Email(email), new Password(randomProvisionedPassword()),
                firstName, lastName, companyId);
        user.linkKeycloak(keycloakSub);
        user.setName((firstName + " " + lastName).trim());

        User saved = userRepository.save(user);

        assignDefaultRole(saved);
        eventPublisher.publish(UserCreatedEvent.create(saved.getId(), saved.getEmail().value(), saved.getCompanyId()));
        log.info("Usuário Keycloak provisionado: {} (sub={})", saved.getEmail().value(), keycloakSub);
        return saved;
    }

    private User findExistingKeycloakUser(String keycloakSub, String email) {
        if (keycloakSub != null && !keycloakSub.isBlank()) {
            Optional<User> bySub = userRepository.findByKeycloakSub(keycloakSub);
            if (bySub.isPresent()) {
                return bySub.get();
            }
        }
        if (email != null) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                return byEmail.get();
            }
        }
        return null;
    }

    private boolean syncKeycloakIdentity(User user, String keycloakSub, String email,
                                         String givenName, String familyName) {
        boolean changed = false;
        if (keycloakSub != null && !keycloakSub.isBlank() && !keycloakSub.equals(user.getKeycloakSub())) {
            user.linkKeycloak(keycloakSub);
            changed = true;
        }
        if (givenName != null && !givenName.isBlank()
                && (user.getFirstName() == null || user.getFirstName().isBlank())) {
            user.setFirstName(givenName);
            changed = true;
        }
        if (familyName != null && !familyName.isBlank()
                && (user.getLastName() == null || user.getLastName().isBlank())) {
            user.setLastName(familyName);
            changed = true;
        }
        if (changed && (user.getName() == null || user.getName().isBlank())) {
            user.setName((user.getFirstName() + " " + user.getLastName()).trim());
        }
        return changed;
    }

    private void rejectIfInactive(User user) {
        if (!user.isActive()) {
            throw new UserProvisioningException("Usuário desativado: contate o administrador.");
        }
    }

    private void assignDefaultRole(User user) {
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(defaultRoleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UserProvisioningException(
                "Role padrão de provisionamento inválida: " + defaultRoleName);
        }

        Optional<Role> roleOpt = roleRepository.findByNameAndCompanyId(roleName, Role.SYSTEM_COMPANY_ID);
        if (roleOpt.isEmpty()) {
            throw new UserProvisioningException(
                "Role padrão não encontrada no banco: " + roleName);
        }
        Role role = roleOpt.get();
        if (userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            return;
        }
        try {
            userRoleRepository.save(UserRole.assign(user.getId(), role.getId(), user.getCompanyId()));
        } catch (DataIntegrityViolationException e) {
            // Atribuição concorrente já realizada por outra requisição do mesmo usuário.
        }
    }

    private UUID resolveDefaultCompanyId() {
        if (defaultCompanyId != null && !defaultCompanyId.isBlank()) {
            try {
                return UUID.fromString(defaultCompanyId);
            } catch (IllegalArgumentException e) {
                throw new UserProvisioningException(
                    "ID da empresa padrão inválido: " + defaultCompanyId);
            }
        }
        return companyRepository.findAll().stream()
                .filter(company -> company.getStatus() == CompanyStatus.ACTIVE)
                .findFirst()
                .map(Company::getId)
                .orElseThrow(() -> new UserProvisioningException(
                    "Não foi possível provisionar o usuário: nenhuma empresa ativa disponível."));
    }

    private String resolveEmail(String email, String preferredUsername) {
        String candidate = email != null && !email.isBlank() ? email : preferredUsername;
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        try {
            new Email(candidate);
            return candidate;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String randomProvisionedPassword() {
        return "Kc!" + UUID.randomUUID() + "Aa1";
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
