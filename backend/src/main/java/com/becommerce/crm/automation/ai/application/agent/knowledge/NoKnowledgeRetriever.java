package com.becommerce.crm.automation.ai.application.agent.knowledge;

import com.becommerce.crm.automation.ai.application.agent.context.KnowledgeContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Sem base de conhecimento cadastrada ainda: nenhum trecho é enviado ao modelo. */
@Component
public class NoKnowledgeRetriever implements KnowledgeRetriever {

    @Override
    public KnowledgeContext retrieve(UUID companyId, UUID agentConfigId, String currentMessage) {
        return KnowledgeContext.EMPTY;
    }
}
