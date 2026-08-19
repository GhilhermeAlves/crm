package com.becommerce.crm.application.ai.context;

import java.util.List;

/**
 * Resultado do Context Engine (AI-02): contexto completo e composto que o
 * assistente usa para responder. Reúne usuário, empresa, permissões, aplicação
 * (tela/rota/módulo) e o registro em foco. {@code record} e {@code crmContext}
 * podem ser {@code null} quando não há registro aplicável ou sem permissão de
 * leitura — nesse caso o assistente responde apenas com os dados disponíveis.
 */
public record ResolvedAiContext(
        AiUserContext user,
        AiCompanyContext company,
        AiPermissionContext permissions,
        AiApplicationContext application,
        AiRecordContext record,
        String crmContext) {

    public static ResolvedAiContext of(AiUserContext user, AiCompanyContext company,
                                       AiPermissionContext permissions,
                                       AiApplicationContext application,
                                       AiRecordContext record, String crmContext) {
        return new ResolvedAiContext(user, company, permissions, application, record, crmContext);
    }

    /** Contexto vazio (sem registro) para quando não há nada a resolver. */
    public static ResolvedAiContext empty(AiUserContext user, AiCompanyContext company,
                                          AiPermissionContext permissions,
                                          AiApplicationContext application) {
        return new ResolvedAiContext(user, company, permissions, application, null, null);
    }
}