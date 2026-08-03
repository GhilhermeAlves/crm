package com.becommerce.auth.application.gateway.service;

import com.becommerce.auth.application.gateway.port.input.GatewayOidcUseCase;
import com.becommerce.auth.application.gateway.port.output.OidcTokenClient;
import com.becommerce.auth.application.identity.port.input.CurrentUserResolutionUseCase;
import com.becommerce.auth.domain.gateway.GatewaySession;
import com.becommerce.auth.domain.gateway.OidcAuthorizationRequest;
import com.becommerce.auth.domain.gateway.OidcGatewayException;
import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUser;
import com.becommerce.auth.domain.identity.CurrentUserResolution;
import com.becommerce.auth.domain.identity.exception.CrmAccessDeniedException;
import com.becommerce.auth.infrastructure.gateway.GatewaySessionStore;
import com.becommerce.auth.infrastructure.gateway.OidcAuthorizationRequestStore;
import com.becommerce.auth.infrastructure.gateway.OidcGatewayProperties;
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
 * Orquestra o fluxo OIDC do Access Gateway (Authorization Code + PKCE S256):
 *
 * <pre>
 * authorize → valida redirect + gera state/nonce/PKCE + monta URL do Keycloak
 * callback → valida state (uso único) → troca code (servidor) → valida tokens
 *            → decide CRM Access → cria sessão de browser (cookie HttpOnly)
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

    public GatewayOidcService(OidcGatewayProperties properties,
                              SecureTokenGenerator tokenGenerator,
                              PkceGenerator pkceGenerator,
                              RedirectUriValidator redirectUriValidator,
                              OidcAuthorizationRequestStore authorizationRequestStore,
                              OidcTokenClient tokenClient,
                              OidcTokenValidator tokenValidator,
                              KeycloakIdentityConverter identityConverter,
                              CurrentUserResolutionUseCase currentUserResolutionUseCase,
                              GatewaySessionStore sessionStore) {
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
        GatewaySession session = createSession(currentUser);
        sessionStore.put(session);
        log.info("OIDC gateway session created: correlation={} user={}", state, session.userId());

        return new AuthenticationResult(session, request.getRedirectTarget());
    }

    private GatewaySession createSession(CurrentUser currentUser) {
        String sessionToken = tokenGenerator.urlSafe(32);
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
                now.plus(properties.getSessionTtl()));
    }

    private AuthenticatedIdentity extractIdentity(Jwt idToken) {
        AbstractAuthenticationToken authentication = identityConverter.convert(idToken);
        return (AuthenticatedIdentity) authentication.getPrincipal();
    }
}
