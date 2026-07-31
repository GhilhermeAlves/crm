package com.becommerce.auth.application.identity.port.input;

import com.becommerce.auth.domain.identity.AuthenticatedIdentity;
import com.becommerce.auth.domain.identity.CurrentUserResolution;

/**
 * Porta de entrada (use case) para resolução do CurrentUser a partir de uma
 * identidade já autenticada (JWT oficial do Keycloak).
 */
public interface CurrentUserResolutionUseCase {

    CurrentUserResolution resolve(AuthenticatedIdentity identity);
}
