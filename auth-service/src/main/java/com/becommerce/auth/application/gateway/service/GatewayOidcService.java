package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.application.gateway.port.input.IdentityProviderCatalog;
import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcAuthorizationRequest;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.PendingLink;
import com.becommerce.auth.domain.gateway.SessionLookup;
import com.becommerce.auth.domain.gateway.SessionStatus;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.auth.infrastructure.gateway.BackendIdentityClient;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionLock;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionResolver;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionStore;
import com.becommerce.auth.infrastructure.gateway.OidcAuthorizationRequestStore;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import com.becommerce.auth.infrastructure.gateway.OidcProviderMetadata;
import com.becommerce.auth.infrastructure.gateway.OidcTokenValidator;
import com.becommerce.auth.infrastructure.gateway.PendingLinkStore;
import com.becommerce.auth.infrastructure.gateway.PkceGenerator;
import com.becommerce.auth.infrastructure.gateway.RedirectUriValidator;
import com.becommerce.auth.infrastructure.gateway.SecureTokenGenerator;
import com.becommerce.auth.infrastructure.security.KeycloakIdentityConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Optional;

/**
 * Orquestra o fluxo OIDC do Access Gateway (Sprints 6.1/6.2):
 *
 * <pre>
 * authorize → valida redirect + gera state/nonce/PKCE + monta URL do Keycloak
 * callback → valida state (uso único) → troca code (servidor) → valida tokens
 *            → decide CRM Access → cria sessão de browser (cookie HttpOnly)
 * logout   → invalida sessão local (idempotente) → monta redirect end_session
 * refresh  → lock por sessão → troca refresh token no servidor (rotação) →
 *            renova lastAccessedAt, respeita TTL absoluto, nunca devolve tokens
 * </pre>
 *
 * <p>A decisão de CRM Access é <b>reutilizada</b> do
 * {@link CurrentUserResolutionUseCase} existente (gate is_active + crm_enabled +
 * company ACTIVE, Sprint 6) — não é duplicada aqui.
 */
@Service
public class GatewayOidcService implements GatewayOidcUseCase {

    private static final Logger log = LoggerFactory.getLogger(GatewayOidcService.class);

    private final OidcGatewayProperties properties;
    private final SecureTokenGenerator tokenGenerator;
    private final PkceGenerator pkceGenerator;
    private final RedirectUriValidator redirectUriValidator;
    private final OidcAuthorizationRequestStore authorizationRequestStore;
    private final OidcTokenClient tokenClient;
    private final OidcTokenValidator tokenValidator;
    private final KeycloakIdentityConverter identityConverter;
    private final CurrentUserResolutionUseCase currentUserResolutionUseCase;
    private final GatewaySessionStore sessionStore;
    private final GatewaySessionResolver sessionResolver;
    private final OidcProviderMetadata providerMetadata;
    private final IdentityProviderCatalog identityProviderCatalog;
    private final BackendIdentityClient backendIdentityClient;
    private final PendingLinkStore pendingLinkStore;

    public GatewayOidcService(OidcGatewayProperties properties,
                              SecureTokenGenerator tokenGenerator,
                              PkceGenerator pkceGenerator,
                              RedirectUriValidator redirectUriValidator,
                              OidcAuthorizationRequestStore authorizationRequestStore,
                              OidcTokenClient tokenClient,
                              OidcTokenValidator tokenValidator,
                              KeycloakIdentityConverter identityConverter,
                              CurrentUserResolutionUseCase currentUserResolutionUseCase,
                              GatewaySessionStore sessionStore,
                              GatewaySessionResolver sessionResolver,
                              OidcProviderMetadata providerMetadata,
                              IdentityProviderCatalog identityProviderCatalog,
                              BackendIdentityClient backendIdentityClient,
                              PendingLinkStore pendingLinkStore) {
        this.properties = properties;
        this.tokenGenerator = tokenGenerator;
        this.pkceGenerator = pkceGenerator;
        this.redirectUriValidator = redirectUriValidator;
        this.authorizationRequestStore = authorizationRequestStore;
        this.tokenClient = tokenClient;
        this.tokenValidator = tokenValidator;
        this.identityConverter = identityConverter;
        this.currentUserResolutionUseCase = currentUserResolutionUseCase;
        this.sessionStore = sessionStore;
        this.sessionResolver = sessionResolver;
        this.providerMetadata = providerMetadata;
        this.identityProviderCatalog = identityProviderCatalog;
        this.backendIdentityClient = backendIdentityClient;
        this.pendingLinkStore = pendingLinkStore;
    }

    @Override
    public BeginAuthorization beginAuthorization(String redirect, String provider) {
        return beginAuthorization(redirect, provider, null);
    }

    @Override
    public BeginAuthorization beginAuthorization(String redirect, String provider, String publicOrigin) {
        String redirectTarget = redirectUriValidator.validateAndNormalize(redirect);

        EffectiveEndpoints endpoints = effectiveEndpoints(publicOrigin);

        String state = tokenGenerator.urlSafe(32);
        String nonce = tokenGenerator.urlSafe(32);
        String codeVerifier = pkceGenerator.codeVerifier();
        String codeChallenge = pkceGenerator.codeChallengeS256(codeVerifier);

        OidcAuthorizationRequest request = new OidcAuthorizationRequest(
                state, nonce, codeVerifier, redirectTarget,
                Instant.now().plus(properties.getAuthorizationRequestTtl()),
                endpoints.baseUrl(), endpoints.redirectUri());
        authorizationRequestStore.put(request);
        log.info("OIDC authorization started: correlation={} redirectUri={}", state, endpoints.redirectUri());

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getAuthorizationEndpoint())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", endpoints.redirectUri())
                .queryParam("scope", properties.getScope())
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256");

        applyIdentityProviderHint(builder, provider);

        String authorizationUri = builder.build().encode().toUriString();

        return new BeginAuthorization(authorizationUri, redirectTarget);
    }

    /**
     * Resolve os endpoints de redirect deste fluxo. Com o modo dinâmico
     * habilitado e uma origem pública derivada do request, o candidato
     * {@code origem + /auth/callback} é aceito somente se a origem estiver na
     * allowlist ({@code allowedRedirectUris}). Qualquer falha (origem fora da
     * allowlist, host ausente/ambíguo, modo desabilitado) cai nos valores fixos
     * configurados — comportamento clássico inalterado.
     */
    private EffectiveEndpoints effectiveEndpoints(String publicOrigin) {
        if (properties.isDynamicRedirectUri() && StringUtils.hasText(publicOrigin)) {
            String candidateBase = publicOrigin.trim().replaceAll("/+$", "");
            String candidate = candidateBase + "/auth/callback";
            try {
                String validated = redirectUriValidator.validateAndNormalize(candidate);
                if (candidate.equals(validated)) {
                    log.info("OIDC effective redirect derived from request origin: origin={}", candidateBase);
                    return new EffectiveEndpoints(candidate, candidateBase);
                }
                log.warn("OIDC candidate redirect not allowlisted, falling back to fixed: candidate={}", candidate);
            } catch (OidcGatewayException e) {
                log.warn("OIDC candidate redirect rejected, falling back to fixed: candidate={} error={}",
                        candidate, e.getCode());
            }
        } else if (StringUtils.hasText(publicOrigin)) {
            log.warn("OIDC dynamic redirect disabled, ignoring request origin: origin={}", publicOrigin);
        }
        return new EffectiveEndpoints(properties.getRedirectUri(), properties.getAppBaseUrl());
    }

    private record EffectiveEndpoints(String redirectUri, String baseUrl) {
    }

    /**
     * Encaminha o usuário direto a um Identity Provider do Keycloak (Identity
     * Brokering, Sprint 7.0) quando o alias informado existe e está habilitado.
     * O alias é validado contra o catálogo no servidor (allowlist): alias
     * desconhecido → {@code 400 UNKNOWN_PROVIDER}; conhecido mas não habilitado
     * → {@code 400 PROVIDER_NOT_AVAILABLE}. Sem provider, o fluxo segue
     * idêntico ao das Sprints 6.x (login local Keycloak).
     */
    private void applyIdentityProviderHint(UriComponentsBuilder builder, String provider) {
        if (!StringUtils.hasText(provider)) {
            return;
        }
        IdentityProviderCatalog.IdentityProviderInfo idp = identityProviderCatalog.find(provider)
                .orElseThrow(() -> new OidcGatewayException("UNKNOWN_PROVIDER", 400,
                        "Provedor de identidade desconhecido."));
        if (!idp.available()) {
            throw new OidcGatewayException("PROVIDER_NOT_AVAILABLE", 400,
                    "Provedor de identidade ainda não configurado.");
        }
        if ("phone".equals(idp.alias())) {
            // Sprint 7.4: telefone é um fluxo local de OTP, NÃO um IdP do
            // Keycloak — nunca deve gerar kc_idp_hint. A tela de login coleta o
            // OTP e segue para o fluxo de senha do Keycloak (sessão do gateway).
            throw new OidcGatewayException("PHONE_IS_LOCAL_FLOW", 400,
                    "Login por telefone usa o fluxo local de OTP; use a tela de login.");
        }
        log.info("OIDC identity provider hint: provider={}", idp.alias());
        builder.queryParam("kc_idp_hint", idp.alias());
    }

    @Override
    public AuthenticationResult completeAuthorization(String code, String state) {
        if (!StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            throw new OidcGatewayException("INVALID_CALLBACK", 400,
                    "Callback inválido: code/state obrigatórios.");
        }
        log.info("OIDC callback received: correlation={}", state);

        OidcAuthorizationRequest request = authorizationRequestStore.consume(state);
        if (request == null) {
            log.warn("OIDC state validation failed (unknown, expired or reused): correlation={}", state);
            throw new OidcGatewayException("INVALID_STATE", 400,
                    "State desconhecido, expirado ou reutilizado.");
        }
        log.info("OIDC state validated (single-use): correlation={}", state);

        OidcTokenClient.TokenResponse tokens;
        try {
            tokens = tokenClient.exchange(new OidcTokenClient.ExchangeRequest(
                    code, request.getCodeVerifier(), request.getRedirectUri()));
        } catch (OidcGatewayException e) {
            log.warn("OIDC token exchange failed: correlation={} error={}", state, e.getCode());
            throw e;
        }
        log.info("OIDC token exchange succeeded: correlation={}", state);

        Jwt idToken = tokenValidator.validateIdToken(tokens.idToken(), request.getNonce());
        tokenValidator.validateAccessToken(tokens.accessToken());
        log.info("OIDC tokens validated: correlation={}", state);

        AuthenticatedIdentity identity = extractIdentity(idToken);
        CurrentUserResolution resolution;
        try {
            resolution = currentUserResolutionUseCase.resolve(identity);
        } catch (CrmAccessDeniedException e) {
            log.warn("OIDC CRM access denied: correlation={} subject={}", state, identity.keycloakSub());
            throw e;
        }
        log.info("OIDC identity resolved: correlation={} subject={}", state, identity.keycloakSub());

        if (resolution instanceof CurrentUserResolution.Resolved resolved) {
            return sessionCreated(resolved.currentUser(), tokens, request.getRedirectTarget(), state);
        }

        if (resolution instanceof CurrentUserResolution.LinkingRequired) {
            // Caso B (Sprint 7.2): e-mail de provedor externo coincide com conta
            // local sem keycloak_sub. Sessão pendente curta → /link-account.
            log.warn("OIDC linking required (email matches local account): correlation={} subject={}",
                    state, identity.keycloakSub());
            return beginPendingLink(identity, tokens, request.getRedirectTarget(), state, request.getPublicBaseUrl());
        }

        // PROVISIONING_REQUIRED
        if (!isExternalProvider(identity.provider())) {
            log.warn("OIDC provisioning required (no CRM user): correlation={} subject={}", state, identity.keycloakSub());
            throw new OidcGatewayException("PROVISIONING_REQUIRED", 403,
                    "Identidade sem usuário CRM: provisionamento é responsabilidade do crm-backend.");
        }

        // Caso C (Sprint 7.2): identidade externa sem conta CRM → auto-provision
        // no backend (endpoint interno, bearer = access token do usuário).
        log.info("OIDC external identity, provisioning: correlation={} subject={}", state, identity.keycloakSub());
        BackendIdentityClient.ProvisionOutcome outcome = backendIdentityClient.provision(tokens.accessToken());
        if (outcome == BackendIdentityClient.ProvisionOutcome.PROVISIONED) {
            CurrentUserResolution reResolved = currentUserResolutionUseCase.resolve(identity);
            if (reResolved instanceof CurrentUserResolution.Resolved resolved) {
                log.info("OIDC provisioned and resolved: correlation={} subject={}", state, identity.keycloakSub());
                return sessionCreated(resolved.currentUser(), tokens, request.getRedirectTarget(), state);
            }
            throw new OidcGatewayException("PROVISIONING_INCOMPLETE", 502,
                    "Identidade provisionada, mas a resolução não foi concluída.");
        }
        // Corrida entre o check de e-mail e a provisão: conta local surgiu no
        // meio → segue o Caso B (verificação explícita).
        log.warn("OIDC provision answered LINKING_REQUIRED: correlation={} subject={}", state, identity.keycloakSub());
        return beginPendingLink(identity, tokens, request.getRedirectTarget(), state, request.getPublicBaseUrl());
    }

    @Override
    public LogoutResult logout(String sessionToken, String postLogoutRedirectUri) {
        return logout(sessionToken, postLogoutRedirectUri, null);
    }

    @Override
    public LogoutResult logout(String sessionToken, String postLogoutRedirectUri, String publicOrigin) {
        String validatedTarget = redirectUriValidator.validateAndNormalize(postLogoutRedirectUri);
        String resolvedTarget = absolutize(validatedTarget, effectiveEndpoints(publicOrigin).baseUrl());

        SessionLookup lookup = sessionResolver.resolve(sessionToken);
        GatewaySession session = lookup.session();
        String idTokenHint = session != null ? session.idTokenHint() : null;

        // A invalidação local SEMPRE acontece (idempotente), mesmo que o provedor
        // esteja indisponível — o logout local nunca fica refém da rede.
        sessionStore.revoke(sessionToken);
        log.info("OIDC gateway session invalidated: user={}", session != null ? session.userId() : "none");

        String endSessionEndpoint;
        try {
            endSessionEndpoint = providerMetadata.endSessionEndpoint();
        } catch (OidcGatewayException e) {
            log.warn("OIDC provider unavailable on logout, redirecting locally: error={}", e.getCode());
            return new LogoutResult(resolvedTarget);
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(endSessionEndpoint)
                .queryParam("client_id", properties.getClientId())
                .queryParam("post_logout_redirect_uri", resolvedTarget);
        if (StringUtils.hasText(idTokenHint)) {
            builder.queryParam("id_token_hint", idTokenHint);
        }
        log.info("OIDC logout redirect built: user={}", session != null ? session.userId() : "none");

        return new LogoutResult(builder.build().encode().toUriString());
    }

    @Override
    public RefreshResult refresh(String sessionToken) {
        SessionLookup initial = sessionResolver.resolve(sessionToken);
        switch (initial.status()) {
            case NOT_FOUND -> throw new OidcGatewayException("SESSION_NOT_FOUND", 401,
                    "Sessão de gateway não encontrada.");
            case EXPIRED -> throw new OidcGatewayException("SESSION_EXPIRED", 401,
                    "Sessão de gateway expirada.");
            case REVOKED -> throw new OidcGatewayException("SESSION_REVOKED", 401,
                    "Sessão de gateway revogada.");
            case ACTIVE -> {
                // segue
            }
        }

        try (GatewaySessionLock ignored = sessionStore.lockFor(sessionToken)) {
            return refreshLocked(sessionToken);
        }
    }

    private RefreshResult refreshLocked(String sessionToken) {
        SessionLookup lookup = sessionResolver.resolve(sessionToken);
        if (lookup.status() == SessionStatus.NOT_FOUND) {
            throw new OidcGatewayException("SESSION_NOT_FOUND", 401, "Sessão de gateway não encontrada.");
        }
        if (lookup.status() == SessionStatus.REVOKED) {
            throw new OidcGatewayException("SESSION_REVOKED", 401, "Sessão de gateway revogada.");
        }
        if (lookup.status() == SessionStatus.EXPIRED) {
            throw new OidcGatewayException("SESSION_EXPIRED", 401, "Sessão de gateway expirada.");
        }

        GatewaySession current = lookup.session();
        if (!StringUtils.hasText(current.refreshToken())) {
            sessionStore.revoke(sessionToken);
            log.warn("OIDC refresh rejected: session without refresh token, session revoked: user={}", current.userId());
            throw new OidcGatewayException("REFRESH_TOKEN_INVALID", 401,
                    "Sessão sem refresh token.");
        }

        log.info("OIDC refresh started: user={}", current.userId());
        OidcTokenClient.TokenResponse tokens;
        try {
            tokens = tokenClient.refresh(new OidcTokenClient.RefreshRequest(current.refreshToken()));
        } catch (OidcGatewayException e) {
            sessionStore.revoke(sessionToken);
            log.warn("OIDC refresh failed, session revoked: user={} error={}", current.userId(), e.getCode());
            throw e;
        }
        log.info("OIDC refresh succeeded: user={}", current.userId());

        Instant now = Instant.now();
        GatewaySession refreshed = current.withRotatedTokens(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.idToken(),
                now.plusSeconds(tokens.expiresInSeconds()),
                now);

        if (!refreshed.isActive(now, properties.getSessionIdleTimeout())) {
            sessionStore.revoke(sessionToken);
            log.warn("OIDC refresh beyond absolute TTL, session revoked: user={}", current.userId());
            throw new OidcGatewayException("SESSION_EXPIRED", 401, "Sessão de gateway expirada.");
        }

        // Confirma que a sessão não foi revogada/removida durante a chamada de rede.
        SessionLookup latest = sessionResolver.resolve(sessionToken);
        if (latest.status() != SessionStatus.ACTIVE) {
            log.warn("OIDC refresh lost race with logout, session revoked: user={}", current.userId());
            throw new OidcGatewayException("SESSION_REVOKED", 401, "Sessão de gateway revogada.");
        }
        sessionStore.put(refreshed);
        return new RefreshResult(refreshed);
    }

    @Override
    public LinkStatusResult linkStatus(String pendingToken) {
        if (!StringUtils.hasText(pendingToken)) {
            return new LinkStatusResult(false, null);
        }
        Optional<PendingLink> pending = pendingLinkStore.get(pendingToken);
        if (pending.isEmpty()) {
            return new LinkStatusResult(false, null);
        }
        return new LinkStatusResult(true, pending.get().email());
    }

    @Override
    public LinkResult completeLink(String pendingToken, String password) {
        if (!StringUtils.hasText(password)) {
            throw new OidcGatewayException("INVALID_LINK_REQUEST", 400, "Senha obrigatória.");
        }
        PendingLink pending = pendingLinkStore.get(pendingToken)
                .orElseThrow(() -> new OidcGatewayException("LINK_PENDING_NOT_FOUND", 410,
                        "Vínculo pendente expirado ou inexistente."));

        BackendIdentityClient.LinkOutcome outcome = backendIdentityClient.link(pending.accessToken(), password);
        switch (outcome) {
            case INVALID_CREDENTIALS -> throw new OidcGatewayException("INVALID_CREDENTIALS", 401,
                    "Senha da conta local inválida.");
            case LINK_NOT_FOUND -> {
                pendingLinkStore.remove(pendingToken);
                throw new OidcGatewayException("LINK_NOT_FOUND", 410,
                        "Conta local não encontrada (pode ter sido removida).");
            }
            case LINKED -> {
                // segue
            }
        }

        pendingLinkStore.remove(pendingToken);
        log.info("Identity link succeeded: subject={} email={}", pending.keycloakSub(), pending.email());

        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                pending.keycloakSub(), pending.email(), null, null, null,
                pending.displayName(), null, pending.provider());
        CurrentUserResolution reResolved = currentUserResolutionUseCase.resolve(identity);
        if (!(reResolved instanceof CurrentUserResolution.Resolved resolved)) {
            throw new OidcGatewayException("LINK_RESOLUTION_FAILED", 502,
                    "Vínculo concluído, mas a identidade não pôde ser resolvida.");
        }
        OidcTokenClient.TokenResponse tokens = new OidcTokenClient.TokenResponse(
                pending.accessToken(), pending.refreshToken(), pending.idToken(),
                Math.max(1, pending.accessTokenExpiresAt().getEpochSecond() - Instant.now().getEpochSecond()));
        GatewaySession session = createSession(resolved.currentUser(), tokens);
        sessionStore.put(session);
        log.info("OIDC gateway session created after link: user={}", session.userId());

        return new LinkResult(pending.redirectTarget(), session);
    }

    private AuthenticationResult sessionCreated(CurrentUser currentUser, OidcTokenClient.TokenResponse tokens,
                                                String redirectTarget, String correlation) {
        GatewaySession session = createSession(currentUser, tokens);
        sessionStore.put(session);
        log.info("OIDC gateway session created: correlation={} user={}", correlation, session.userId());
        return new AuthenticationResult(session, null, redirectTarget);
    }

    private AuthenticationResult beginPendingLink(AuthenticatedIdentity identity,
                                                  OidcTokenClient.TokenResponse tokens,
                                                  String redirectTarget, String correlation,
                                                  String publicBaseUrl) {
        String token = tokenGenerator.urlSafe(32);
        String csrfToken = tokenGenerator.urlSafe(32);
        Instant now = Instant.now();
        PendingLink pendingLink = new PendingLink(
                token,
                identity.keycloakSub(),
                identity.email(),
                identity.displayName(),
                identity.provider(),
                csrfToken,
                tokens.idToken(),
                tokens.accessToken(),
                tokens.refreshToken(),
                now.plusSeconds(tokens.expiresInSeconds()),
                redirectTarget,
                now,
                now.plus(properties.getPendingLinkTtl()));
        pendingLinkStore.put(pendingLink);
        log.info("OIDC pending link started: correlation={} subject={}", correlation, identity.keycloakSub());

        String base = StringUtils.hasText(publicBaseUrl) ? publicBaseUrl : properties.getAppBaseUrl();
        String linkAccountUri = base.replaceAll("/+$", "") + "/link-account";
        return new AuthenticationResult(null,
                new PendingLinkInfo(pendingLink.token(), pendingLink.email(), pendingLink.csrfToken(), redirectTarget),
                linkAccountUri);
    }

    private GatewaySession createSession(CurrentUser currentUser, OidcTokenClient.TokenResponse tokens) {
        String sessionToken = tokenGenerator.urlSafe(32);
        String csrfToken = tokenGenerator.urlSafe(32);
        Instant now = Instant.now();
        return new GatewaySession(
                sessionToken,
                currentUser.userId(),
                currentUser.email(),
                currentUser.companyId(),
                currentUser.tenantId(),
                currentUser.roles(),
                currentUser.permissions(),
                currentUser.keycloakSub(),
                currentUser.sessionId(),
                currentUser.provider(),
                currentUser.displayName(),
                now,
                now.plus(properties.getSessionTtl()),
                now,
                tokens.idToken(),
                tokens.accessToken(),
                tokens.refreshToken(),
                now.plusSeconds(tokens.expiresInSeconds()),
                csrfToken,
                null);
    }

    private String absolutize(String target, String base) {
        if (StringUtils.hasText(base) && target.startsWith("/") && !target.startsWith("//")) {
            return base.replaceAll("/+$", "") + target;
        }
        return target;
    }

    private AuthenticatedIdentity extractIdentity(Jwt idToken) {
        AbstractAuthenticationToken authentication = identityConverter.convert(idToken);
        return (AuthenticatedIdentity) authentication.getPrincipal();
    }

    private boolean isExternalProvider(String provider) {
        return provider != null && !provider.isBlank() && !"keycloak".equalsIgnoreCase(provider);
    }
}
