package com.becommerce.crm.automation.ai.application.agent;

import com.becommerce.crm.automation.ai.application.agent.context.AgentContext;
import com.becommerce.crm.automation.ai.application.agent.context.AgentRuntimeInput;
import com.becommerce.crm.automation.ai.application.agent.context.ClinicContext;
import com.becommerce.crm.automation.ai.application.agent.context.ClinicContextProvider;
import com.becommerce.crm.automation.ai.application.agent.context.ConversationHistory;
import com.becommerce.crm.automation.ai.application.agent.context.KnowledgeContext;
import com.becommerce.crm.automation.ai.application.agent.context.MemoryContext;
import com.becommerce.crm.automation.ai.application.agent.context.PatientContext;
import com.becommerce.crm.automation.ai.application.agent.context.PatientContextProvider;
import com.becommerce.crm.automation.ai.application.agent.knowledge.KnowledgeRetriever;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolbox;
import com.becommerce.crm.automation.ai.application.agent.tool.ToolContext;
import com.becommerce.crm.automation.ai.application.memory.AgentMemoryService;
import com.becommerce.crm.automation.ai.application.memory.MemoryQuery;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ponto ÚNICO de montagem do contexto de execução do agente:
 * configuração (identidade/comportamento) + contexto da clínica + contexto do
 * paciente + histórico recente + memórias relevantes + conhecimento +
 * ferramentas disponíveis + mensagem atual. Cada parte vem da sua fonte
 * (CRM, memória, knowledge, toolbox); falha em uma parte opcional degrada só
 * aquela parte.
 */
@Component
public class AgentContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(AgentContextBuilder.class);

    private final ClinicContextProvider clinicProvider;
    private final PatientContextProvider patientProvider;
    private final AgentMemoryService memoryService;
    private final KnowledgeRetriever knowledgeRetriever;
    private final AgentToolbox toolbox;

    public AgentContextBuilder(ClinicContextProvider clinicProvider, PatientContextProvider patientProvider,
                               AgentMemoryService memoryService, KnowledgeRetriever knowledgeRetriever,
                               AgentToolbox toolbox) {
        this.clinicProvider = clinicProvider;
        this.patientProvider = patientProvider;
        this.memoryService = memoryService;
        this.knowledgeRetriever = knowledgeRetriever;
        this.toolbox = toolbox;
    }

    /**
     * Builder sem fontes do CRM (sem clínica/paciente/memória/conhecimento) —
     * usado só por construtores legados do runtime e em testes.
     */
    public static AgentContextBuilder withoutCrmContext(AgentToolbox toolbox) {
        return new AgentContextBuilder(ClinicContextProvider.none(), PatientContextProvider.none(), null,
                (companyId, agentConfigId, message) -> KnowledgeContext.EMPTY, toolbox);
    }

    public AgentContext build(AgentConfig agent, AgentRuntimeInput input) {
        ClinicContext clinic = clinicProvider.load(input.companyId());
        PatientContext patient = patientProvider.load(input.companyId(), input.contactId(), input.phone(),
                input.senderName(), clinic);
        MemoryContext memory = memories(agent, input, patient);
        KnowledgeContext knowledge = knowledge(agent, input);
        ToolContext tools = toolbox.available(agent, toolSession(agent, input, patient));
        ConversationHistory history = input.history() == null ? ConversationHistory.EMPTY : input.history();
        return new AgentContext(agent, clinic, patient, history, memory, knowledge, tools, input.currentMessage());
    }

    /** Sessão das ferramentas: escopo sempre do runtime, nunca dos argumentos do modelo. */
    public AgentToolSession toolSession(AgentConfig agent, AgentRuntimeInput input, PatientContext patient) {
        return new AgentToolSession(input.companyId(), agent.getId(), input.conversationId(),
                patient.contactId(), input.phone(), input.senderName(), input.inboundMessageId());
    }

    private MemoryContext memories(AgentConfig agent, AgentRuntimeInput input, PatientContext patient) {
        if (memoryService == null || !agent.isMemoryEnabled() || !patient.isIdentified()) {
            return MemoryContext.DISABLED;
        }
        try {
            return new MemoryContext(true, memoryService.retrieveRelevant(new MemoryQuery(input.companyId(),
                    agent.getId(), patient.contactId(), input.currentMessage(), MemoryQuery.DEFAULT_LIMIT)));
        } catch (RuntimeException e) {
            log.warn("Memórias indisponíveis (company={}): {}", input.companyId(), e.getMessage());
            return new MemoryContext(true, java.util.List.of());
        }
    }

    private KnowledgeContext knowledge(AgentConfig agent, AgentRuntimeInput input) {
        try {
            return knowledgeRetriever.retrieve(input.companyId(), agent.getId(), input.currentMessage());
        } catch (RuntimeException e) {
            log.warn("Conhecimento indisponível (company={}): {}", input.companyId(), e.getMessage());
            return KnowledgeContext.EMPTY;
        }
    }
}
