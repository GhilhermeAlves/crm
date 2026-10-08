package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolProvider;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.sales.scheduling.domain.AppointmentType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Ferramentas de agenda ({@code consultar_horarios_livres}, {@code agendar_consulta})
 * no contrato comum {@link AgentToolProvider}. Disponível só quando a empresa
 * tem tipos de consulta ativos com profissional. A lógica continua em
 * {@link WhatsAppSchedulingTools}.
 */
@Component
public class SchedulingToolProvider implements AgentToolProvider {

    private final WhatsAppSchedulingTools schedulingTools;

    public SchedulingToolProvider(WhatsAppSchedulingTools schedulingTools) {
        this.schedulingTools = schedulingTools;
    }

    @Override
    public String id() {
        return "agenda";
    }

    @Override
    public boolean isAvailable(AgentConfig agent, AgentToolSession session) {
        return !schedulingTools.bookableTypes(session.companyId()).isEmpty();
    }

    @Override
    public List<AiProvider.ToolDefinition> definitions() {
        return schedulingTools.definitions();
    }

    @Override
    public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        List<AppointmentType> types = schedulingTools.bookableTypes(session.companyId());
        return types.isEmpty() ? Optional.empty() : Optional.of(schedulingTools.guidance(types));
    }

    @Override
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        return schedulingTools.execute(new WhatsAppSchedulingTools.Context(session.companyId(),
                session.conversationId(), session.phone(), session.senderName()), call);
    }
}
