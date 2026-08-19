package com.becommerce.crm.application.ai.context;

import java.util.UUID;

/**
 * Contexto da empresa ativa (AI-02). Derivado do {@code CurrentUser}
 * autenticado; o RLS garante que apenas dados da empresa são acessíveis.
 */
public record AiCompanyContext(UUID companyId) {
}
