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
 *
 * <p><b>LINKING_REQUIRED</b> (Sprint 7.2): identidade de provedor externo
 * (ex.: Google, via Identity Brokering) cujo e-mail coincide com uma conta
 * local existente, porém sem {@code keycloak_sub} vinculado. NUNCA se resolve
 * nem se auto-vincula por e-mail — o usuário precisa vincular explicitamente
 * verificando a senha da conta local.
 */
public sealed interface CurrentUserResolution
        permits CurrentUserResolution.Resolved, CurrentUserResolution.ProvisioningRequired,
        CurrentUserResolution.LinkingRequired {

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

    record LinkingRequired(AuthenticatedIdentity identity) implements CurrentUserResolution {
        public LinkingRequired {
            java.util.Objects.requireNonNull(identity, "identity");
        }
    }
}
