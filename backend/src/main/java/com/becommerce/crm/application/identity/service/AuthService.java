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
import com.becommerce.crm.application.membership.port.output.MembershipRepository;
import com.becommerce.crm.domain.company.Company;
import com.becommerce.crm.domain.identity.PasswordResetToken;
import com.becommerce.crm.domain.identity.Role;
import com.becommerce.crm.domain.identity.User;
import com.becommerce.crm.domain.identity.UserRole;
import com.becommerce.crm.domain.identity.event.PasswordChangedEvent;
import com.becommerce.crm.domain.identity.event.PasswordResetRequestedEvent;
import com.becommerce.crm.domain.identity.event.UserCreatedEvent;
import com.becommerce.crm.domain.identity.exception.InvalidCredentialsException;
import com.becommerce.crm.domain.identity.exception.InvalidTokenException;
import com.becommerce.crm.domain.identity.exception.LinkingRequiredException;
import com.becommerce.crm.domain.identity.exception.UserNotFoundException;
import com.becommerce.crm.domain.identity.exception.UserProvisioningException;
import com.becommerce.crm.domain.identity.valueobject.Email;
import com.becommerce.crm.domain.identity.valueobject.Password;
import com.becommerce.crm.domain.identity.valueobject.RoleName;
import com.becommerce.crm.domain.membership.Membership;
import com.becommerce.crm.infrastructure.identity.client.AuthServiceClient;
import com.becommerce.crm.infrastructure.tenant.context.TenantContext;
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
    private final MembershipRepository membershipRepository;
    private final CompanyRepository companyRepository;
    private final CrmAccessService crmAccessService;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;

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
                       MembershipRepository membershipRepository,
                       CrmAccessService crmAccessService,
                       PasswordEncoder passwordEncoder, EventPublisher eventPublisher,
                       EmailService emailService, AuthServiceClient authServiceClient) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.membershipRepository = membershipRepository;
        this.companyRepository = companyRepository;
        this.crmAccessService = crmAccessService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.emailService = emailService;
        this.authServiceClient = authServiceClient;
    }

    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already exists");
        }

        Email email = new Email(request.email());
        Password password = new Password(request.password());

        UUID companyId = resolveCompanyForRegistration(request.companyId());
        User user = User.create(email, password, request.name(), "", companyId);
        userRepository.save(user);

        eventPublisher.publish(UserCreatedEvent.create(user.getId(), request.email(), companyId));
    }

    @Override
    public void forgotPassword(String email) {
        // Endpoint anônimo: sem JWT, sem tenant. O email informado é a única
        // identidade disponível — o bootstrap da linha em `users` via
        // `app.current_identity_email` (V025) permite a leitura da PRÓPRIA
        // conta pelo email (RLS FORCE). O GUC não expõe senha/hash; só localiza.
        TenantContext.setIdentityEmail(email);
        try {
            userRepository.findByEmail(email).ifPresent(user -> {
                String token = UUID.randomUUID().toString();
                PasswordResetToken resetToken = PasswordResetToken.create(token, user.getId(), RESET_TOKEN_EXPIRY_MINUTES);
                // O INSERT em password_reset_tokens (RLS FORCE, V027) exige o GUC
                // app.current_reset_token igual ao token recém-gerado — define-se
                // aqui, no escopo da requisição, e limpa-se no finally.
                TenantContext.setResetToken(token);
                try {
                    passwordResetTokenRepository.save(resetToken);
                } finally {
                    TenantContext.clearResetToken();
                }
                eventPublisher.publish(PasswordResetRequestedEvent.create(user.getId(), user.getCompanyId(), email, token));
                log.info("Reset de senha solicitado para: {} (token gerado; envio via de envio configurada)", email);
                emailService.sendPasswordResetEmail(email, token);
            });
        } finally {
            TenantContext.clearIdentityEmail();
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // Endpoint anônimo: sem JWT/company_id. O token de reset é o segredo de
        // posse — o datasource define `app.current_reset_token` (V027) para o
        // bootstrap das leituras de token/usuário e da atualização do token em
        // `password_reset_tokens` e `users` (RLS FORCE).
        TenantContext.setResetToken(token);
        try {
            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

            if (!resetToken.isValid()) {
                throw new InvalidTokenException("Reset token has expired or already been used");
            }

            User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(UserNotFoundException::new);

            Password password = new Password(newPassword);

            // Sprint 7.4: quando a conta é proveniente do Keycloak, o reset REAL
            // acontece no Keycloak (auth-service → Keycloak Admin), a única fonte
            // de verdade da credencial. Contas criadas localmente (fallback pré
            // identity-layer) seguem o fluxo legado de hash próprio.
            if (user.getKeycloakSub() != null && !user.getKeycloakSub().isBlank()) {
                authServiceClient.resetPassword(user.getKeycloakSub(), user.getEmail().value(), newPassword);
                log.info("Credencial de usuário resetada no Keycloak (sub={})", user.getKeycloakSub());
            } else {
                user.updatePassword(password);
                userRepository.save(user);
            }

            resetToken.markAsUsed();
            passwordResetTokenRepository.save(resetToken);

            eventPublisher.publish(PasswordChangedEvent.create(user.getId(), user.getCompanyId()));
        } finally {
            TenantContext.clearResetToken();
        }
    }

    @Override
    @Transactional
    public User provisionKeycloakUser(String keycloakSub, String email, String preferredUsername,
                                      String givenName, String familyName) {
        return provisionKeycloakUser(keycloakSub, email, preferredUsername, givenName, familyName, null);
    }

    @Override
    @Transactional
    public User provisionKeycloakUser(String keycloakSub, String email, String preferredUsername,
                                      String givenName, String familyName, String provider) {
        String resolvedEmail = resolveEmail(email, preferredUsername);
        boolean externalProvider = isExternalProvider(provider);

        if (!provisioningEnabled) {
            // Provedor externo: um match por e-mail jamais resolve sem vinculação
            // explícita, mesmo com provisionamento desabilitado.
            if (externalProvider && resolvedEmail != null) {
                Optional<User> byEmail = userRepository.findByEmail(resolvedEmail);
                if (byEmail.isPresent()) {
                    throw new LinkingRequiredException(
                        "Conta local existente para o e-mail informado: vinculação exige verificação explícita.");
                }
            }
            User existing = findExistingKeycloakUser(keycloakSub, resolvedEmail);
            if (existing != null) {
                crmAccessService.assertCrmAccess(existing);
                return existing;
            }
            throw new UserProvisioningException(
                "Auto-provisioning de usuários do Keycloak está desabilitado.");
        }

        // Caso A / idempotência: já vinculado pelo sub.
        if (keycloakSub != null && !keycloakSub.isBlank()) {
            Optional<User> bySub = userRepository.findByKeycloakSub(keycloakSub);
            if (bySub.isPresent()) {
                User resolved = syncAndResolve(bySub.get(), keycloakSub, resolvedEmail, givenName, familyName);
                return resolved;
            }
        }

        // Match por e-mail com conta local existente.
        if (resolvedEmail != null) {
            Optional<User> byEmail = userRepository.findByEmail(resolvedEmail);
            if (byEmail.isPresent()) {
                if (externalProvider) {
                    // Sprint 7.2: NUNCA auto-vincular identidade externa a uma
                    // conta local apenas pelo e-mail — exige verificação explícita.
                    throw new LinkingRequiredException(
                        "Conta local existente para o e-mail informado: vinculação exige verificação explícita.");
                }
                User resolved = syncAndResolve(byEmail.get(), keycloakSub, resolvedEmail, givenName, familyName);
                return resolved;
            }
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
            User created = self.createProvisionedUser(keycloakSub, resolvedEmail, givenName, familyName);
            if (created.getCompanyId() != null) {
                crmAccessService.assertCrmAccess(created);
            }
            return created;
        } catch (DataIntegrityViolationException e) {
            User raced = findExistingKeycloakUser(keycloakSub, resolvedEmail);
            if (raced != null) {
                return syncAndResolve(raced, keycloakSub, resolvedEmail, givenName, familyName);
            }
            throw new UserProvisioningException(
                "Não foi possível provisionar o usuário após conflito de criação: " + resolvedEmail);
        }
    }

    /**
     * Vincula (link) a identidade externa à conta local do e-mail (Caso B,
     * Sprint 7.2) após VERIFICAR a senha da conta local. O gate de segurança é a
     * senha + o e-mail verificado pelo provedor; a política RLS V024 habilita
     * a leitura/update da própria linha por {@code app.current_identity_email}.
     *
     * <p>Idempotente: sub já vinculado → retorna a conta sem re-verificar senha
     * (replay/login repetido não duplica nem exige senha de novo).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User linkKeycloakIdentity(String keycloakSub, String email, String givenName, String familyName,
                                     String rawPassword) {
        if (keycloakSub == null || keycloakSub.isBlank()) {
            throw new UserProvisioningException(
                "Vinculação de identidade exige subject (sub) do Keycloak.");
        }

        TenantContext.setKeycloakSub(keycloakSub);
        TenantContext.setIdentityEmail(email);
        try {
            Optional<User> bySub = userRepository.findByKeycloakSub(keycloakSub);
            if (bySub.isPresent()) {
                User resolved = syncAndResolve(bySub.get(), keycloakSub, email, givenName, familyName);
                return resolved;
            }

            if (email == null || email.isBlank()) {
                throw new UserProvisioningException(
                    "Vinculação de identidade exige e-mail do JWT do Keycloak.");
            }

            User local = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserProvisioningException(
                    "Conta local não encontrada para vinculação (pode ter sido removida)."));

            if (rawPassword == null || rawPassword.isBlank()
                    || !passwordEncoder.matches(rawPassword, local.getPassword().value())) {
                throw new InvalidCredentialsException("Senha da conta local inválida.");
            }

            User resolved = syncAndResolve(local, keycloakSub, email, givenName, familyName);
            log.info("Identidade externa vinculada à conta local: email={} sub={}", email, keycloakSub);
            return resolved;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Aplica a sincronização de identidade ({@link #syncKeycloakIdentity}) e o
     * gate de acesso ao CRM. Retorna o usuário persistido quando houve mudança.
     *
     * <p>Sprint 8.3: usuário provisionado SEM empresa (company_id null) pula o
     * gate de acesso ao CRM (onboarding pendente) — não há empresa para checar;
     * ele é resolvido como autenticado-sem-empresa e direcionado ao onboarding.
     */
    private User syncAndResolve(User user, String keycloakSub, String email,
                                String givenName, String familyName) {
        boolean changed = syncKeycloakIdentity(user, keycloakSub, email, givenName, familyName);
        User resolved = changed ? userRepository.save(user) : user;
        if (resolved.getCompanyId() != null) {
            crmAccessService.assertCrmAccess(resolved);
        }
        return resolved;
    }

    private boolean isExternalProvider(String provider) {
        return provider != null && !provider.isBlank() && !"keycloak".equalsIgnoreCase(provider);
    }

    /**
     * Cria o usuário provisionado a partir do Keycloak.
     *
     * <p>Sob RLS FORCE, o INSERT em {@code users} exige
     * {@code company_id = app.current_tenant_id()} (WITH CHECK). O tenant do
     * usuário novo vem EXCLUSIVAMENTE de fonte confiável
     * ({@code app.auth.provisioning.default-company-id}); se não estiver
     * configurado, o provisionamento NÃO inventa um tenant e sinaliza
     * {@code PROVISIONING_REQUIRED} explicitamente.
     *
     * <p>O {@code company_id} é definido no {@link TenantContext} antes do
     * primeiro acesso ao banco, para que o GUC {@code app.current_company_id}
     * seja aplicado à conexão da transação (o datasource aplica o GUC na
     * aquisição da conexão, no primeiro statement JDBC). Assim o
     * {@code WITH CHECK} é satisfeito legitimamente para o usuário recém-criado,
     * sem bypass de RLS — mesmo padrão do RoleDataSeeder.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User createProvisionedUser(String keycloakSub, String email, String givenName, String familyName) {
        UUID companyId = resolveDefaultCompanyId();
        if (companyId != null) {
            // Sprint 8.3: quando há empresa padrão configurada, o tenant é
            // definido no TenantContext antes do INSERT para satisfazer o
            // WITH CHECK do tenant_isolation_policy (V019). Sem empresa padrão,
            // a linha é criada com company_id NULL pela
            // identity_onboarding_insert_policy (V032) — onboarding pendente.
            TenantContext.setCompanyId(companyId);
        }
        String firstName = givenName != null && !givenName.isBlank() ? givenName : email.substring(0, email.indexOf('@'));
        String lastName = familyName != null && !familyName.isBlank() ? familyName : "";

        User user = User.create(new Email(email), new Password(randomProvisionedPassword()),
                firstName, lastName, companyId);
        user.linkKeycloak(keycloakSub);
        user.setName((firstName + " " + lastName).trim());

        User saved = userRepository.save(user);

        if (companyId != null) {
            assignDefaultRole(saved);
            // Sprint 8.2: membership é a fonte de verdade da relação usuário↔empresa.
            // O trigger de consistência mantém users.company_id (aqui já definido).
            if (!membershipRepository.existsActiveByUserIdAndCompanyId(saved.getId(), companyId)) {
                membershipRepository.save(Membership.activate(saved.getId(), companyId, defaultRoleName.trim().toUpperCase()));
            }
        }
        eventPublisher.publish(UserCreatedEvent.create(saved.getId(), saved.getEmail().value(), saved.getCompanyId()));
        log.info("Usuário Keycloak provisionado: {} (sub={}){}", saved.getEmail().value(), keycloakSub,
                companyId == null ? " [sem empresa — onboarding pendente]" : "");
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

    private void assignDefaultRole(User user) {
        RoleName roleName;
        try {
            roleName = RoleName.valueOf(defaultRoleName.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UserProvisioningException(
                "Role padrão de provisionamento inválida: " + defaultRoleName);
        }

        Optional<Role> roleOpt = roleRepository.findByNameAndCompanyId(roleName.name(), user.getCompanyId());
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

    private UUID resolveCompanyForRegistration(UUID requestedCompanyId) {
        if (requestedCompanyId != null) {
            return companyRepository.findById(requestedCompanyId)
                    .map(Company::getId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Empresa informada não existe: " + requestedCompanyId));
        }
        return resolveDefaultCompanyId();
    }

    /**
     * Resolve o tenant de provisionamento de forma confiável: apenas a fonte
     * explícita {@code app.auth.provisioning.default-company-id}. Não há
     * fallback para "primeira empresa ativa", pois escolheria um tenant
     * arbitrariamente (proibido).
     *
     * <p>Sprint 8.3: se o tenant não for determinável, retorna {@code null}
     * (em vez de lançar {@code PROVISIONING_REQUIRED}) — o usuário passa a ser
     * provisionado SEM empresa e é direcionado ao onboarding self-service
     * (criação da primeira empresa). {@code AUTH_DEFAULT_COMPANY_ID} continua
     * válido como fallback opcional quando configurado (D4).
     */
    private UUID resolveDefaultCompanyId() {
        if (defaultCompanyId == null || defaultCompanyId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(defaultCompanyId);
        } catch (IllegalArgumentException e) {
            throw new UserProvisioningException(
                "ID da empresa padrão inválido: " + defaultCompanyId);
        }
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
