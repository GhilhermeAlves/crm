package com.becommerce.auth.domain.identity;

/**
 * Resultado da resolução do usuário autenticado.
 *
 * <p><b>RESOLVED</b>: identidade autenticada mapeada para um {@code CurrentUser}
 * completo (usuário + empresa/tenant + roles + permissions).
 *
 * <p><b>PROVISIONING_REQUIRED</b>: identidade autenticada válida, porém sem
 * usuário correspondente no banco CRM. Contrato preparado para o provisionamento
 * automático (hoje responsabilidade do crm-backend — Sprint 1; migra para o
 * auth-service em sprint posterior). O {@code identity} é ecoado apenas porque
 * já foi derivado do JWT autenticado.
 */
public sealed interface CurrentUserResolution
        permits CurrentUserResolution.Resolved, CurrentUserResolution.ProvisioningRequired {

    record Resolved(CurrentUser currentUser) implements CurrentUserResolution {
        public Resolved {
            java.util.Objects.requireNonNull(currentUser, "currentUser");
        }
    }

    record ProvisioningRequired(AuthenticatedIdentity identity) implements CurrentUserResolution {
        public ProvisioningRequired {
            java.util.Objects.requireNonNull(identity, "identity");
        }
    }
}
