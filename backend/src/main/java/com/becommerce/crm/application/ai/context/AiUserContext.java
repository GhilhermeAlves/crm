package com.becommerce.crm.application.ai.context;

import java.util.UUID;

/**
 * Contexto do usuário (AI-02). Sempre derivado do {@code CurrentUser}
 * autenticado — nunca de campos enviados pelo frontend.
 */
public record AiUserContext(UUID userId) {
}
