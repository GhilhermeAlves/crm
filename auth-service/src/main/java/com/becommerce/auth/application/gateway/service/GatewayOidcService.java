package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcAuthorizationRequest;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.gateway.SessionLookup;
import com.becommerce.auth.domain.gateway.SessionStatus;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionResolver;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionStore;
import com.becommerce.auth.infrastructure.gateway.OidcAuthorizationRequestStore;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
import com.becommerce.auth.infrastructure.gateway.OidcProviderMetadata;
import com.becommerce.auth.infrastructure.gateway.OidcTokenValidator;
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
                              OidcProviderMetadata providerMetadata) {
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
    }

    @Override
    public BeginAuthorization beginAuthorization(String redirect) {
        String redirectTarget = redirectUriValidator.validateAndNormalize(redirect);

        String state = tokenGenerator.urlSafe(32);
        String nonce = tokenGenerator.urlSafe(32);
        String codeVerifier = pkceGenerator.codeVerifier();
        String codeChallenge = pkceGenerator.codeChallengeS256(codeVerifier);

        OidcAuthorizationRequest request = new OidcAuthorizationRequest(
                state, nonce, codeVerifier, redirectTarget,
                Instant.now().plus(properties.getAuthorizationRequestTtl()));
        authorizationRequestStore.put(request);
        log.info("OIDC authorization started: correlation={}", state);

        String authorizationUri = UriComponentsBuilder.fromHttpUrl(properties.getAuthorizationEndpoint())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("scope", properties.getScope())
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .build()
                .encode()
                .toUriString();

        return new BeginAuthorization(authorizationUri, redirectTarget);
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
            tokens = tokenClient.exchange(new OidcTokenClient.ExchangeRequest(code, request.getCodeVerifier()));
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

        if (resolution instanceof CurrentUserResolution.ProvisioningRequired) {
            log.warn("OIDC provisioning required (no CRM user): correlation={} subject={}", state, identity.keycloakSub());
            throw new OidcGatewayException("PROVISIONING_REQUIRED", 403,
                    "Identidade sem usuário CRM: provisionamento é responsabilidade do crm-backend.");
        }

        CurrentUser currentUser = ((CurrentUserResolution.Resolved) resolution).currentUser();
        GatewaySession session = createSession(currentUser, tokens);
        sessionStore.put(session);
        log.info("OIDC gateway session created: correlation={} user={}", state, session.userId());

        return new AuthenticationResult(session, request.getRedirectTarget());
    }

    @Override
    public LogoutResult logout(String sessionToken, String postLogoutRedirectUri) {
        String validatedTarget = redirectUriValidator.validateAndNormalize(postLogoutRedirectUri);
        String resolvedTarget = absolutize(validatedTarget);

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

        Object lock = sessionStore.lockFor(sessionToken);
        synchronized (lock) {
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

    private String absolutize(String target) {
        String base = properties.getAppBaseUrl();
        if (StringUtils.hasText(base) && target.startsWith("/") && !target.startsWith("//")) {
            return base.replaceAll("/+$", "") + target;
        }
        return target;
    }

    private AuthenticatedIdentity extractIdentity(Jwt idToken) {
        AbstractAuthenticationToken authentication = identityConverter.convert(idToken);
        return (AuthenticatedIdentity) authentication.getPrincipal();
    }
}
