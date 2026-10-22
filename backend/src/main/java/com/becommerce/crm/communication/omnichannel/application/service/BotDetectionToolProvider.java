package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolProvider;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code marcar_como_robo}: o próprio agente percebe que conversa com outro
 * robô (menus, respostas que ignoram o que foi dito, saudações repetidas) e
 * para — o número vai para a lista de ignorados via {@link AutomatedSenderGuard}.
 * Complementa a detecção automática do {@link AutomatedSenderDetector}.
 */
@Component
public class BotDetectionToolProvider implements AgentToolProvider {

    static final String MARK_BOT = "marcar_como_robo";

    /** Resposta que o processador reconhece para não enviar nada ao robô. */
    static final String SILENCE = "__SILENCIO__";

    private final AutomatedSenderGuard guard;

    public BotDetectionToolProvider(AutomatedSenderGuard guard) {
        this.guard = guard;
    }

    @Override
    public String id() {
        return "bot_detection";
    }

    @Override
    public boolean isAvailable(AgentConfig agent, AgentToolSession session) {
        return session.conversationId() != null && session.phone() != null;
    }

    @Override
    public List<AiProvider.ToolDefinition> definitions() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("motivo", Map.of("type", "string",
                        "description", "Por que parece um robô/atendimento automático")),
                "required", List.of("motivo"));
        return List.of(new AiProvider.ToolDefinition(MARK_BOT,
                "Encerra a conversa com outro robô ou atendimento automático: o número passa a ser ignorado.",
                schema));
    }

    @Override
    public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        return Optional.of("""
                Robôs e respostas automáticas (marcar_como_robo):
                - Se quem escreve for outro robô (menu de opções, "mensagem automática", assistente virtual, \
                respostas que ignoram o que você disse ou a mesma saudação repetida), \
                NÃO tente agendar nem continuar: chame marcar_como_robo.
                - Depois de chamar, responda exatamente __SILENCIO__ (nada será enviado).
                - Na dúvida se é uma pessoa, continue normalmente.""");
    }

    @Override
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        Object reason = call.arguments() == null ? null : call.arguments().get("motivo");
        String motivo = reason == null ? "identificado pelo agente" : "agente: " + reason;
        guard.ignore(session.companyId(), session.conversationId(), session.phone(), motivo);
        return "Número ignorado. Responda exatamente " + SILENCE + " para não enviar nada.";
    }
}
