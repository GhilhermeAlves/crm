package com.becommerce.crm.automation.ai.application.agent.tool;

import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de um grupo de ferramentas REAIS do agente (agenda, transferência
 * humana, memória…). Cada provider decide se está disponível para a empresa/
 * interação, expõe as definições estruturadas (tool calling) e executa.
 * Só existem ferramentas para funcionalidades que o CRM realmente tem.
 */
public interface AgentToolProvider {

    /** Identificador estável do grupo (ex.: {@code agenda}). */
    String id();

    boolean isAvailable(AgentConfig agent, AgentToolSession session);

    List<AiProvider.ToolDefinition> definitions();

    /** Regras de uso das ferramentas (curtas); vazio quando não há. */
    default Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        return Optional.empty();
    }

    default boolean handles(String toolName) {
        return definitions().stream().anyMatch(d -> d.name().equals(toolName));
    }

    /** Executa e devolve o resultado em texto para o modelo. Nunca lança. */
    String execute(AgentToolSession session, AiProvider.ToolCall call);
}
