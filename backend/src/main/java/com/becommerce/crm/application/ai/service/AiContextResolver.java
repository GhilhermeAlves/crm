package com.becommerce.crm.application.ai.service;

import com.becommerce.crm.application.ai.context.AiApplicationContext;
import com.becommerce.crm.application.ai.context.AiCompanyContext;
import com.becommerce.crm.application.ai.context.AiPermissionContext;
import com.becommerce.crm.application.ai.context.AiRecordContext;
import com.becommerce.crm.application.ai.context.AiRecordContextResolver;
import com.becommerce.crm.application.ai.context.AiUserContext;
import com.becommerce.crm.application.ai.context.ResolvedAiContext;
import com.becommerce.crm.application.ai.dto.AiContextPayload;
import com.becommerce.crm.domain.ai.AiRecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Context Engine (AI-02): dispatcher que resolve o contexto completo a partir
 * do {@code CurrentUser} autenticado (companyId/userId/permissions) e das dicas
 * de tela/rota/registro enviadas pelo frontend. O {@code AiContextPayload}
 * apenas direciona; os dados reais vêm do CRM, nunca do payload.
 *
 * <p>Para cada tipo de registro em foco, o dispatcher seleciona o
 * {@link AiRecordContextResolver} correspondente e só o executa se o usuário
 * possuir a permissão de leitura exigida. Sem permissão (ou sem registro),
 * o contexto de CRM é omitido — o assistente responde apenas com o que é
 * permitido ver.</p>
 */
@Component
public class AiContextResolver {

    private static final Logger log = LoggerFactory.getLogger(AiContextResolver.class);

    private final Map<AiRecordType, AiRecordContextResolver> resolvers = new EnumMap<>(AiRecordType.class);

    public AiContextResolver(List<AiRecordContextResolver> resolvers) {
        for (AiRecordContextResolver r : resolvers) {
            this.resolvers.put(r.type(), r);
        }
    }

    /**
     * Resolve o contexto completo para uma interação do assistente.
     *
     * @param companyId empresa ativa (do CurrentUser)
     * @param userId usuário autenticado (do CurrentUser)
     * @param permissions permissões do usuário (do CurrentUser)
     * @param context dica de navegação/registro enviada pelo frontend (pode ser null)
     */
    public ResolvedAiContext resolve(UUID companyId, UUID userId, List<String> permissions,
                                     AiContextPayload context) {
        AiUserContext user = new AiUserContext(userId);
        AiCompanyContext company = new AiCompanyContext(companyId);
        AiPermissionContext permissionContext = new AiPermissionContext(permissions);
        AiApplicationContext application = AiApplicationContext.of(
                context != null ? context.screen() : null,
                context != null ? context.route() : null);

        if (context == null || context.recordId() == null) {
            return ResolvedAiContext.empty(user, company, permissionContext, application);
        }

        AiRecordType type = context.resolvedType();
        if (type == null) {
            return ResolvedAiContext.empty(user, company, permissionContext, application);
        }

        AiRecordContextResolver resolver = resolvers.get(type);
        if (resolver == null) {
            return ResolvedAiContext.empty(user, company, permissionContext, application);
        }

        if (!permissionContext.has(resolver.requiredPermission())) {
            log.info("Usuário {} sem permissão {}; contexto de {} omitido.",
                    userId, resolver.requiredPermission(), type);
            return ResolvedAiContext.empty(user, company, permissionContext, application);
        }

        String crmContext = resolver.resolve(companyId, context.recordId());
        if (crmContext == null) {
            return ResolvedAiContext.empty(user, company, permissionContext, application);
        }

        return ResolvedAiContext.of(user, company, permissionContext, application,
                new AiRecordContext(type, context.recordId()), crmContext);
    }
}