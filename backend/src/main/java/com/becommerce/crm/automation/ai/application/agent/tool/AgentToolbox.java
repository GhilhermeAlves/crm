package com.becommerce.crm.automation.ai.application.agent.tool;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reúne os {@link AgentToolProvider}s registrados e resolve quais estão
 * disponíveis para uma interação. Um provider com falha na checagem é apenas
 * omitido (a conversa segue sem ele).
 */
@Component
public class AgentToolbox {

    private static final Logger log = LoggerFactory.getLogger(AgentToolbox.class);

    private final List<AgentToolProvider> providers;

    public AgentToolbox(List<AgentToolProvider> providers) {
        this.providers = providers == null ? List.of() : List.copyOf(providers);
    }

    public ToolContext available(AgentConfig agent, AgentToolSession session) {
        List<AgentToolProvider> active = new ArrayList<>();
        List<AiProvider.ToolDefinition> definitions = new ArrayList<>();
        List<String> guidance = new ArrayList<>();
        for (AgentToolProvider provider : providers) {
            try {
                if (!provider.isAvailable(agent, session)) {
                    continue;
                }
                active.add(provider);
                definitions.addAll(provider.definitions());
                provider.guidance(agent, session).ifPresent(guidance::add);
            } catch (RuntimeException e) {
                log.warn("Ferramenta {} indisponível (company={}): {}", provider.id(), session.companyId(),
                        e.getMessage());
            }
        }
        return new ToolContext(active, definitions, guidance);
    }
}
