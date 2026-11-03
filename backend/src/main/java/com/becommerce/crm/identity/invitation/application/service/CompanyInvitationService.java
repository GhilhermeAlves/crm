package com.becommerce.crm.identity.invitation.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.communication.notification.application.EmailSender;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.application.service.AuthService;
import com.becommerce.crm.identity.domain.User;
import com.becommerce.crm.identity.domain.exception.DuplicateEmailException;
import com.becommerce.crm.identity.domain.exception.UserNotFoundException;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.CompanyDetails;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.CompanyPrefill;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.CreateRequest;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.LinkResponse;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.PreviewResponse;
import com.becommerce.crm.identity.invitation.application.dto.CompanyInvitationDtos.Response;
import com.becommerce.crm.identity.invitation.application.port.output.CompanyInvitationRepository;
import com.becommerce.crm.identity.invitation.domain.CompanyInvitation;
import com.becommerce.crm.identity.invitation.domain.InvitationStatus;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationConflictException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNoLongerValidException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationNotFoundException;
import com.becommerce.crm.identity.invitation.domain.exception.InvitationRuleViolationException;
import com.becommerce.crm.identity.invitation.infrastructure.persistence.InvitationTokenContextHolder;
import com.becommerce.crm.identity.invitation.infrastructure.rate.InvitationRateLimiter;
import com.becommerce.crm.masterdata.company.application.dto.CompanyResponse;
import com.becommerce.crm.masterdata.company.application.dto.CreateCompanyRequest;
import com.becommerce.crm.masterdata.company.application.port.input.CompanyUseCase;
import com.becommerce.crm.masterdata.company.domain.CompanyModule;
import com.becommerce.crm.masterdata.company.domain.CompanyModules;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import com.becommerce.crm.masterdata.company.domain.CompanySegment;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Convite de criação de empresa (V123). Só o CRM_ADMIN cria (garantido no
 * controller); ele fixa segmento, módulos, plano e limite de usuários — o
 * convidado não escolhe nada disso.
 *
 * <p>No aceite, a empresa é criada pelo mesmo provisionamento do cadastro de
 * empresa ({@link CompanyUseCase#createCompany}): o convidado vira dono
 * (membership OWNER, papel RBAC ADMIN) e o CRM_ADMIN não entra como membro.
 * O cadastro de quem ainda não tem conta é orquestrado por
 * {@link CompanyInvitationSignupService} (Keycloak + compensação).
 */
@Service
public class CompanyInvitationService {

    private final CompanyInvitationRepository repository;
    private final CompanyUseCase companyUseCase;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final EmailSender emailSender;
    private final InvitationTokenContextHolder tokenContext;
    private final InvitationRateLimiter rateLimiter;
    private final TenantAuditRecorder auditor;
    private final String invitationBaseUrl;

    public CompanyInvitationService(CompanyInvitationRepository repository,
                                    CompanyUseCase companyUseCase,
                                    UserRepository userRepository,
                                    AuthService authService,
                                    EmailSender emailSender,
                                    InvitationTokenContextHolder tokenContext,
                                    InvitationRateLimiter rateLimiter,
                                    TenantAuditRecorder auditor,
                                    @Value("${app.invitations.base-url:}") String invitationBaseUrl) {
        this.repository = repository;
        this.companyUseCase = companyUseCase;
        this.userRepository = userRepository;
        this.authService = authService;
        this.emailSender = emailSender;
        this.tokenContext = tokenContext;
        this.rateLimiter = rateLimiter;
        this.auditor = auditor;
        this.invitationBaseUrl = invitationBaseUrl;
    }

    @Transactional
    public LinkResponse create(UUID inviterCompanyId, CreateRequest request, UUID invitedBy) {
        if (!rateLimiter.tryCreate("company-invitations:" + inviterCompanyId)) {
            throw new InvitationConflictException("Excesso de convites na janela atual. Tente novamente mais tarde.");
        }
        String segment = parseSegment(request.segment()).name();
        String modules = new CompanyModules(CompanySegment.valueOf(segment), parseModules(request.enabledModules()))
                .toCsv();
        String plan = parsePlan(request.plan()).name();
        String email = normalize(request.email());

        TenantContext.setCompanyId(inviterCompanyId);
        boolean pending = repository.findByInviterCompanyId(inviterCompanyId).stream()
                .anyMatch(i -> i.effectiveStatus() == InvitationStatus.PENDING && i.getEmail().equalsIgnoreCase(email));
        if (pending) {
            throw new InvitationRuleViolationException("Já existe convite de empresa pendente para " + email);
        }

        String token = InvitationTokenService.generateToken();
        Map<String, String> prefill = request.company() == null ? null : request.company().toMap();
        CompanyInvitation invitation = repository.save(CompanyInvitation.create(inviterCompanyId, email,
                blankToNull(request.name()), segment, modules, plan, request.maxUsers(), prefill,
                InvitationTokenService.hash(token), invitedBy));

        String url = buildTokenUrl(token);
        if (request.shouldSendEmail()) {
            emailSender.sendInvitation(email, "uma nova empresa no CRM", "ADMIN", url);
        }
        auditor.record(inviterCompanyId, AuditAction.CREATE, AuditModule.INVITATIONS, "CompanyInvitation",
                invitation.getId().toString(), "Convite de criação de empresa para " + email, invitedBy,
                Map.of("email", email, "segment", segment, "plan", plan,
                        "sent", String.valueOf(request.shouldSendEmail())));
        return new LinkResponse(toResponse(invitation), url);
    }

    @Transactional(readOnly = true)
    public List<Response> list(UUID inviterCompanyId) {
        TenantContext.setCompanyId(inviterCompanyId);
        return repository.findByInviterCompanyId(inviterCompanyId).stream()
                .map(CompanyInvitationService::toResponse).toList();
    }

    @Transactional
    public void revoke(UUID inviterCompanyId, UUID invitationId) {
        TenantContext.setCompanyId(inviterCompanyId);
        CompanyInvitation invitation = findOwn(inviterCompanyId, invitationId);
        invitation.revoke();
        repository.save(invitation);
    }

    @Transactional
    public LinkResponse regenerate(UUID inviterCompanyId, UUID invitationId, boolean sendEmail) {
        TenantContext.setCompanyId(inviterCompanyId);
        CompanyInvitation invitation = findOwn(inviterCompanyId, invitationId);
        String token = InvitationTokenService.generateToken();
        invitation.renew(InvitationTokenService.hash(token));
        CompanyInvitation saved = repository.save(invitation);
        String url = buildTokenUrl(token);
        if (sendEmail) {
            emailSender.sendInvitation(saved.getEmail(), "uma nova empresa no CRM", "ADMIN", url);
        }
        return new LinkResponse(toResponse(saved), url);
    }

    @Transactional(readOnly = true)
    public PreviewResponse preview(String token) {
        return withToken(token, () -> {
            CompanyInvitation i = repository.findByTokenHash(InvitationTokenService.hash(token))
                    .orElseThrow(CompanyInvitationService::notFound);
            InvitationStatus status = i.effectiveStatus();
            boolean hasAccount = status == InvitationStatus.PENDING && accountExists(i.getEmail());
            return new PreviewResponse(i.getEmail(), i.getInviteeName(), i.getSegment(), modules(i),
                    i.getPlan(), i.getMaxUsers(), status, i.getExpiresAt(), hasAccount,
                    status == InvitationStatus.PENDING ? CompanyPrefill.fromMap(i.getCompanyPrefill()) : null);
        });
    }

    /** Quem já tem conta (logado com o e-mail do convite) cria a empresa. */
    @Transactional
    public Response accept(String token, CompanyDetails details, UUID userId) {
        if (!rateLimiter.tryAccept(userId.toString())) {
            throw new InvitationConflictException("Muitas tentativas de aceite. Tente novamente mais tarde.");
        }
        return withToken(token, () -> {
            CompanyInvitation invitation = lockUsable(token);
            User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
            String userEmail = user.getEmail() == null ? "" : user.getEmail().value();
            if (!invitation.getEmail().equalsIgnoreCase(userEmail)) {
                throw new InvitationRuleViolationException(
                        "Este convite é destinado ao e-mail " + invitation.getEmail() + ".");
            }
            return createCompanyFor(invitation, details, user);
        });
    }

    /** Pré-validação do cadastro, ANTES de criar o usuário no Keycloak. */
    @Transactional(readOnly = true)
    public Response prepareSignup(String token) {
        return withToken(token, () -> {
            CompanyInvitation invitation = repository.findByTokenHash(InvitationTokenService.hash(token))
                    .orElseThrow(CompanyInvitationService::notFound);
            assertUsable(invitation);
            if (accountExists(invitation.getEmail())) {
                throw accountAlreadyExists();
            }
            return toResponse(invitation);
        });
    }

    /**
     * Grava o cadastro: usuário do CRM, empresa provisionada e aceite, numa
     * transação aberta por {@link CompanyInvitationSignupService}.
     */
    @Transactional
    public Response completeSignup(String token, String keycloakUserId, String encodedPassword,
                                   String name, CompanyDetails details) {
        try {
            return withToken(token, () -> {
                CompanyInvitation invitation = lockUsable(token);
                String email = invitation.getEmail();
                // Identidade do novo usuário: INSERT em users sem empresa (V032) e
                // leitura da própria linha (V025).
                TenantContext.setKeycloakSub(keycloakUserId);
                TenantContext.setIdentityEmail(email);
                if (userRepository.existsByEmail(email)) {
                    throw accountAlreadyExists();
                }
                User user = authService.provisionInvitedUser(keycloakUserId, email, encodedPassword, name);
                return createCompanyFor(invitation, details, user);
            });
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Cria a empresa com o que o CRM_ADMIN fixou no convite e os dados do
     * convidado; o convidado vira dono (OWNER + RBAC ADMIN).
     */
    private Response createCompanyFor(CompanyInvitation invitation, CompanyDetails d, User user) {
        String keycloakSub = TenantContext.getKeycloakSub();
        String identityEmail = user.getEmail() == null ? invitation.getEmail() : user.getEmail().value();
        TenantContext.setIdentityEmail(identityEmail);

        CreateCompanyRequest request = new CreateCompanyRequest(
                d.legalName().trim(), d.tradingName().trim(), blankToNull(d.cnpj()), null, null,
                normalize(d.email()), d.phone().trim(), null,
                d.addressZipCode().trim(), d.addressStreet().trim(), d.addressNumber().trim(),
                blankToNull(d.addressComplement()), d.addressNeighborhood().trim(),
                d.addressCity().trim(), d.addressState().trim(), null,
                invitation.getPlan(), invitation.getMaxUsers(), null, null, null, null,
                invitation.getSegment(), modules(invitation));
        CompanyResponse company = companyUseCase.createCompany(request, user.getId());
        UUID companyId = UUID.fromString(company.id());

        // O provisionamento limpa o TenantContext. Restaura a identidade para
        // garantir a empresa ativa de quem ainda não tinha nenhuma.
        TenantContext.setCompanyId(companyId);
        if (keycloakSub != null) {
            TenantContext.setKeycloakSub(keycloakSub);
        }
        TenantContext.setIdentityEmail(identityEmail);
        User owner = userRepository.findById(user.getId()).orElse(user);
        owner.grantCrmAccess();
        if (owner.getCompanyId() == null) {
            owner.setCompanyId(companyId);
        }
        userRepository.save(owner);

        invitation.accept(companyId);
        Response response = toResponse(repository.save(invitation));
        auditor.record(companyId, AuditAction.CREATE, AuditModule.INVITATIONS, "CompanyInvitation",
                invitation.getId().toString(), "Empresa criada por convite de " + invitation.getEmail(),
                user.getId(), Map.of("email", invitation.getEmail()));
        return response;
    }

    private CompanyInvitation lockUsable(String token) {
        CompanyInvitation invitation = repository.findByTokenHashForUpdate(InvitationTokenService.hash(token))
                .orElseThrow(CompanyInvitationService::notFound);
        assertUsable(invitation);
        return invitation;
    }

    private static void assertUsable(CompanyInvitation invitation) {
        if (!invitation.isPending()) {
            throw new InvitationNoLongerValidException(invitation.getStatus());
        }
        if (invitation.isExpired()) {
            throw new InvitationNoLongerValidException(InvitationStatus.EXPIRED);
        }
    }

    private <T> T withToken(String token, Supplier<T> action) {
        tokenContext.setTokenHash(InvitationTokenService.hash(token));
        try {
            return action.get();
        } finally {
            tokenContext.clear();
        }
    }

    /** Checa conta existente sob a identidade do e-mail (RLS V025: só a própria linha). */
    private boolean accountExists(String email) {
        String previous = TenantContext.getIdentityEmail();
        TenantContext.setIdentityEmail(email);
        try {
            return userRepository.existsByEmail(email);
        } finally {
            if (previous == null) {
                TenantContext.clearIdentityEmail();
            } else {
                TenantContext.setIdentityEmail(previous);
            }
        }
    }

    private CompanyInvitation findOwn(UUID inviterCompanyId, UUID invitationId) {
        return repository.findById(invitationId)
                .filter(i -> i.getInviterCompanyId().equals(inviterCompanyId))
                .orElseThrow(() -> new InvitationNotFoundException("Convite não encontrado: " + invitationId));
    }

    private static CompanySegment parseSegment(String value) {
        try {
            return CompanySegment.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvitationRuleViolationException("Especialidade inválida: " + value);
        }
    }

    private static java.util.Set<CompanyModule> parseModules(List<String> names) {
        try {
            return CompanyModules.parse(names);
        } catch (RuntimeException e) {
            throw new InvitationRuleViolationException("Módulo inválido: " + e.getMessage());
        }
    }

    private static CompanyPlan parsePlan(String value) {
        try {
            return CompanyPlan.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvitationRuleViolationException("Plano inválido: " + value);
        }
    }

    private static List<String> modules(CompanyInvitation i) {
        return CompanyModules.parseCsv(i.getEnabledModules()).stream().map(Enum::name).sorted().toList();
    }

    private static InvitationNotFoundException notFound() {
        return new InvitationNotFoundException("Convite inválido ou inexistente.");
    }

    private static DuplicateEmailException accountAlreadyExists() {
        return new DuplicateEmailException(
                "Já existe uma conta com este e-mail. Entre com ela para aceitar o convite.");
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String buildTokenUrl(String token) {
        if (invitationBaseUrl != null && !invitationBaseUrl.isBlank()) {
            return invitationBaseUrl.replaceAll("/+$", "") + "/convite-empresa/" + token;
        }
        return "/convite-empresa/" + token;
    }

    private static Response toResponse(CompanyInvitation i) {
        return new Response(i.getId(), i.getEmail(), i.getInviteeName(), i.getSegment(), modules(i), i.getPlan(),
                i.getMaxUsers(), i.effectiveStatus(), i.getCompanyId(), i.getInvitedBy(), i.getExpiresAt(),
                i.getCreatedAt(), CompanyPrefill.fromMap(i.getCompanyPrefill()));
    }
}
