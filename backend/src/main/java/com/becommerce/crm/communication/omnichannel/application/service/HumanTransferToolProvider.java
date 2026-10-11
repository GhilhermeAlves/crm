package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolProvider;
import com.becommerce.crm.automation.ai.application.agent.tool.AgentToolSession;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.communication.omnichannel.application.port.output.OmnichannelConversationRepository;
import com.becommerce.crm.communication.omnichannel.domain.Conversation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code transferir_para_humano}: coloca a conversa em modo humano por um
 * prazo — a IA autônoma para de responder até alguém devolver ao automático
 * ou o prazo vencer. Opt-in por empresa
 * ({@code agent_config.human_transfer_enabled}).
 */
@Component
public class HumanTransferToolProvider implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(HumanTransferToolProvider.class);

    static final String TRANSFER = "transferir_para_humano";

    static final long DEFAULT_PAUSE_HOURS = 12;

    private final OmnichannelConversationRepository conversationRepository;
    private final Duration pause;

    HumanTransferToolProvider(OmnichannelConversationRepository conversationRepository) {
        this(conversationRepository, DEFAULT_PAUSE_HOURS);
    }

    /**
     * A transferência pausa a IA por um prazo (como a resposta do dono pelo
     * celular): se ninguém da equipe assumir, a conversa volta ao automático
     * em vez de ficar sem resposta para sempre.
     */
    @Autowired
    public HumanTransferToolProvider(OmnichannelConversationRepository conversationRepository,
                                     @Value("${omnichannel.whatsapp.human-transfer-pause-hours:12}") long pauseHours) {
        this.conversationRepository = conversationRepository;
        this.pause = Duration.ofHours(Math.max(1, pauseHours));
    }

    @Override
    public String id() {
        return "human_transfer";
    }

    @Override
    public boolean isAvailable(AgentConfig agent, AgentToolSession session) {
        return agent.isHumanTransferEnabled() && session.conversationId() != null;
    }

    @Override
    public List<AiProvider.ToolDefinition> definitions() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("motivo", Map.of("type", "string",
                        "description", "Motivo curto da transferência")),
                "required", List.of("motivo"));
        return List.of(new AiProvider.ToolDefinition(TRANSFER,
                "Transfere a conversa para um atendente humano da equipe; a IA para de responder nesta conversa.",
                schema));
    }

    @Override
    public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        return Optional.of("""
                Transferência humana (transferir_para_humano):
                - Use quando o paciente pedir para falar com uma pessoa, em reclamações, urgências ou quando você não puder resolver.
                - Depois de transferir, avise em uma frase que um atendente vai continuar a conversa.""");
    }

    @Override
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        try {
            Optional<Conversation> conversation = conversationRepository.findById(session.conversationId())
                    .filter(c -> session.companyId().equals(c.getCompanyId()));
            if (conversation.isEmpty()) {
                return "Não foi possível transferir agora. Diga que a equipe vai retornar em breve.";
            }
            Conversation c = conversation.get();
            c.pauseAutomationUntil(LocalDateTime.now().plus(pause));
            conversationRepository.save(c);
            Object reason = call.arguments() == null ? null : call.arguments().get("motivo");
            log.info("[AGENT][HUMAN] conversa transferida (company={}, conversation={}, motivo={})",
                    session.companyId(), session.conversationId(), reason);
            return "Conversa transferida para a equipe. Avise o paciente, em uma frase, que um atendente vai continuar.";
        } catch (RuntimeException e) {
            log.warn("[AGENT][HUMAN] falha ao transferir (company={}): {}", session.companyId(), e.getMessage());
            return "Não foi possível transferir agora. Diga que a equipe vai retornar em breve.";
        }
    }
}
