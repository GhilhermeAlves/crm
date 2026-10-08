package com.becommerce.crm.automation.ai.application.agent.tool;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;

import java.util.List;

/**
 * Ferramentas disponíveis nesta interação: definições estruturadas (enviadas
 * no campo {@code tools} do provider, não como texto) + regras de uso.
 */
public record ToolContext(List<AgentToolProvider> providers, List<AiProvider.ToolDefinition> definitions,
                          List<String> guidance) {

    public static final ToolContext NONE = new ToolContext(List.of(), List.of(), List.of());

    public ToolContext {
        providers = providers == null ? List.of() : List.copyOf(providers);
        definitions = definitions == null ? List.of() : List.copyOf(definitions);
        guidance = guidance == null ? List.of() : List.copyOf(guidance);
    }

    public boolean isEmpty() {
        return definitions.isEmpty();
    }

    /** Despacha a chamada para o provider dono da ferramenta. Nunca lança. */
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        for (AgentToolProvider provider : providers) {
            if (provider.handles(call.name())) {
                return provider.execute(session, call);
            }
        }
        return "Ferramenta indisponível: " + call.name();
    }
}
