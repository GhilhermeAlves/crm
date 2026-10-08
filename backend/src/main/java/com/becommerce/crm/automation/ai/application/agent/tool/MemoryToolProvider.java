package com.becommerce.crm.automation.ai.application.agent.tool;

import com.becommerce.crm.automation.ai.application.memory.AgentMemoryService;
import com.becommerce.crm.automation.ai.application.port.output.AiProvider;
import com.becommerce.crm.automation.ai.domain.AgentConfig;
import com.becommerce.crm.automation.ai.domain.MemorySource;
import com.becommerce.crm.automation.ai.domain.MemoryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@code registrar_memoria}: o modelo PROPÕE uma memória duradoura e a
 * {@code MemoryWritePolicy} decide. Só disponível com memória habilitada e
 * contato identificado no CRM.
 */
@Component
public class MemoryToolProvider implements AgentToolProvider {

    private static final Logger log = LoggerFactory.getLogger(MemoryToolProvider.class);

    static final String REMEMBER = "registrar_memoria";

    private final AgentMemoryService memoryService;

    public MemoryToolProvider(AgentMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String id() {
        return "memory";
    }

    @Override
    public boolean isAvailable(AgentConfig agent, AgentToolSession session) {
        return agent.isMemoryEnabled() && session.contactId() != null;
    }

    @Override
    public List<AiProvider.ToolDefinition> definitions() {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "tipo", Map.of("type", "string", "enum", List.of("PREFERENCE", "FACT", "PROFILE", "CONTEXTUAL"),
                                "description", "PREFERENCE=preferência; FACT=fato; PROFILE=interesse/perfil; "
                                        + "CONTEXTUAL=pendência em aberto"),
                        "conteudo", Map.of("type", "string", "description",
                                "Uma frase curta na 3ª pessoa. Ex.: Prefere atendimento pela manhã."),
                        "importancia", Map.of("type", "integer", "minimum", 1, "maximum", 5)),
                "required", List.of("tipo", "conteudo"));
        return List.of(new AiProvider.ToolDefinition(REMEMBER,
                "Registra uma informação DURADOURA e útil sobre o paciente para atendimentos futuros.", schema));
    }

    @Override
    public Optional<String> guidance(AgentConfig agent, AgentToolSession session) {
        return Optional.of("""
                Memória (registrar_memoria):
                - Registre apenas informações duradouras e úteis no futuro (preferências, fatos, interesses, pendências).
                - NÃO registre cumprimentos, perguntas gerais, agendamentos, horários ou status (a agenda do CRM já guarda isso), nem documentos pessoais.
                - Não avise o paciente que registrou algo.""");
    }

    @Override
    public String execute(AgentToolSession session, AiProvider.ToolCall call) {
        try {
            Map<String, Object> args = call.arguments() == null ? Map.of() : call.arguments();
            MemoryType type = MemoryType.parse(String.valueOf(args.get("tipo")));
            String content = args.get("conteudo") == null ? null : String.valueOf(args.get("conteudo"));
            Integer importance = parseImportance(args.get("importancia"));
            AgentMemoryService.SaveOutcome outcome = memoryService.save(session.companyId(),
                    session.agentConfigId(), session.contactId(), type, content, importance,
                    MemorySource.AGENT, session.inboundMessageId());
            return outcome.saved() ? "Memória registrada." : "Memória não registrada: " + outcome.rejectionReason();
        } catch (RuntimeException e) {
            log.warn("[AGENT][MEMORY] falha ao registrar memória (company={}): {}", session.companyId(),
                    e.getMessage());
            return "Memória não registrada.";
        }
    }

    private static Integer parseImportance(Object value) {
        if (value instanceof Number n) {
            return Math.max(1, Math.min(5, n.intValue()));
        }
        try {
            return value == null ? null : Math.max(1, Math.min(5, Integer.parseInt(String.valueOf(value).trim())));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
