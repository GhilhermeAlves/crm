package com.becommerce.auth.infrastructure.security;

import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Converte o JWT oficial do Keycloak (já validado pelo resource server via JWKS
 * do Keycloak) na identidade autenticada usada pela resolução do CurrentUser.
 *
 * <p>Este serviço não possui JWKS próprio nem emite tokens: apenas extrai claims
 * de uma identidade já autenticada.
 */
@Component
public class KeycloakIdentityConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String displayName = jwt.getClaimAsString("name");
        if ((displayName == null || displayName.isBlank()) && givenName != null && !givenName.isBlank()) {
            displayName = (givenName + " " + (familyName == null ? "" : familyName)).trim();
        }

        AuthenticatedIdentity identity = new AuthenticatedIdentity(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                givenName,
                familyName,
                displayName,
                jwt.getClaimAsString("sid"),
                jwt.getClaimAsString("identity_provider"));

        return new UsernamePasswordAuthenticationToken(identity, jwt, List.of());
    }
}
