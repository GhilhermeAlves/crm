package com.becommerce.crm.application.ai.context;

import java.util.List;

/**
 * Contexto de permissões do usuário (AI-02). Determinado pelo {@code CurrentUser}
 * autenticado. O Context Engine usa essa lista para decidir se expõe dados de um
 * tipo de registro (gate de leitura) — nunca confia no payload.
 */
public record AiPermissionContext(List<String> permissions) {

    public AiPermissionContext {
        permissions = List.copyOf(permissions);
    }

    /** {@code true} se o usuário possui a permissão informada. */
    public boolean has(String permission) {
        return permissions.contains(permission);
    }
}