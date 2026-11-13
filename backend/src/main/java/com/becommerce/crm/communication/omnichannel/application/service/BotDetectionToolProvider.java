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
 * {@code deixar_com_equipe}: o agente percebe que quem escreve não é um paciente
 * (outro robô, ou uma mensagem pessoal/fora do consultório: amigos, família,
 * igreja, fornecedores) e para de responder — a conversa fica com a equipe via
 * {@link AutomatedSenderGuard#handToTeam}. O número NÃO vai para a lista de
 * ignorados: isso só acontece com sinais fortes do {@link AutomatedSenderDetector}.
 * (Antes a ferramenta ignorava o número e pessoas reais sumiam do atendimento.)
 */
@Component
public class BotDetectionToolProvider implements AgentToolProvider {

    static final String HAND_TO_TEAM = "deixar_com_equipe";

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
                        "description", "Por que não parece um paciente (robô ou assunto pessoal)")),
                "required", List.of("motivo"));
        return List.of(new AiProvider.ToolDefinition(HAND_TO_TEAM,
                "Para de responder nesta conversa e deixa com a equipe do consultório (robôs e mensagens "
                        + "que não são de pacientes). Nada é enviado e a equipe é avisada.",
                schema));
    }

    @Override
    public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        return Optional.of("""
                Mensagens que não são de pacientes (deixar_com_equipe):
                - Este WhatsApp também recebe mensagens pessoais de quem trabalha no consultório. \
                Se a mensagem for pessoal ou não tiver relação com atendimento (amigos, família, igreja, \
                escola, fornecedores, comprovantes ou cobranças, propaganda), NÃO responda: chame deixar_com_equipe.
                - Faça o mesmo se for outro robô (menu de opções, "mensagem automática", assistente virtual).
                - Áudio, figurinha, reação ou saudação curta NÃO indicam robô: são pessoas.
                - Depois de chamar, responda exatamente __SILENCIO__ (nada será enviado).
                - Na dúvida se é um paciente, responda normalmente.""");
    }

    @Override
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        Object reason = call.arguments() == null ? null : call.arguments().get("motivo");
        String motivo = reason == null ? "identificado pelo agente" : "agente: " + reason;
        guard.handToTeam(session.companyId(), session.conversationId(), session.phone(), motivo);
        return "Conversa deixada com a equipe. Responda exatamente " + SILENCE + " para não enviar nada.";
    }
}
