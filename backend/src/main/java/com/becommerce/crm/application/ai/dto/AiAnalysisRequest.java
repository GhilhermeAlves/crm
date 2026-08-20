package com.becommerce.crm.application.ai.dto;

import com.becommerce.crm.application.ai.context.AiPermissionContext;
import com.becommerce.crm.domain.ai.AiRecordType;

import java.util.UUID;

/**
 * Requisição da análise contextual (AI-06). A identidade (empresa/usuário/
 * permissões) é sempre derivada do {@code CurrentUser} pelo backend — nunca do
 * payload. O {@code context} apenas direciona qual dado carregar.
 */
public record AiAnalysisRequest(
        String question,
        AiContextPayload context
) {

    /**
     * Tipo de registro em foco, normalizado; {@code null} se ausente/desconhecido.
     * Permissão de leitura exigida para expor dados de cada tipo, alinhada ao
     * Context Engine (AI-02).
     */
    public String requiredPermission() {
        AiRecordType type = context != null ? context.resolvedType() : null;
        if (type == null) {
            return null;
        }
        return switch (type) {
            case OPPORTUNITY -> "opportunity:read";
            case CUSTOMER, CONTACT -> "contact:read";
            case ACTIVITY -> "activity:read";
            case TASK -> "task:read";
        };
    }

    /**
     * {@code true} se o usuário pode acessar os dados do registro em foco.
     * O backend permanece a autoridade sobre permissões.
     */
    public boolean hasPermission(AiPermissionContext permissions) {
        String required = requiredPermission();
        return required == null || permissions.has(required);
    }

    public UUID recordId() {
        return context != null ? context.recordId() : null;
    }
}