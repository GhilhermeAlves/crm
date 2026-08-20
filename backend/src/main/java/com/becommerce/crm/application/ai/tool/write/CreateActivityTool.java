package com.becommerce.crm.application.ai.tool.write;

import com.becommerce.crm.application.ai.action.AiActionService;
import com.becommerce.crm.application.ai.dto.AiActionResponse;
import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.application.ai.tool.AiTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.domain.activity.ActivityType;
import com.becommerce.crm.domain.contact.exception.ContactNotFoundException;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.application.ai.tool.write.ToolArgument.dateTime;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.enumValue;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.put;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.text;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code create_activity} (AI-05). Nao registra a atividade: valida
 * os argumentos e a propriedade dos vinculos (contact/opportunity da MESMA
 * empresa ativa), monta uma descricao amigavel e PERSISTE uma proposta com os
 * parametros tipados. A escrita real so ocorre apos confirmacao explicita do
 * usuario. Requer {@code activity:create}.
 */
@Component
public class CreateActivityTool implements AiTool {

    private static final String NAME = "create_activity";

    private final AiActionService actionService;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;

    public CreateActivityTool(@Lazy AiActionService actionService,
                              ContactRepository contactRepository,
                              OpportunityRepository opportunityRepository) {
        this.actionService = actionService;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe o registro de uma atividade comercial (ligacao, reuniao, e-mail, etc.). "
                + "Nao executa o registro: cria uma proposta que o usuario deve confirmar antes de "
                + "ser efetivada. Use quando o usuario pedir para registrar uma atividade/interacao.";
    }

    @Override
    public String requiredPermission() {
        return "activity:create";
    }

    @Override
    public String risk() {
        return "WRITE";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "type", Map.of("type", "string", "enum", List.of(
                        "CALL", "MEETING", "EMAIL", "MESSAGE", "NOTE", "PROPOSAL", "FOLLOW_UP", "OTHER")),
                "subject", Map.of("type", "string", "description", "Assunto da atividade (obrigatorio, max 255)"),
                "contactId", Map.of("type", "string", "format", "uuid"),
                "opportunityId", Map.of("type", "string", "format", "uuid"),
                "description", Map.of("type", "string"),
                "activityAt", Map.of("type", "string", "format", "date-time")));
        schema.put("required", List.of("type", "subject"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        ActivityType type = enumValue(args, "type", ActivityType.class);
        if (type == null) {
            throw new IllegalArgumentException("O tipo da atividade e obrigatorio.");
        }
        String subject = text(args, "subject");
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("O assunto da atividade e obrigatorio.");
        }
        if (subject.length() > 255) {
            throw new IllegalArgumentException("O assunto deve ter no maximo 255 caracteres.");
        }
        UUID contactId = uuid(args, "contactId");
        UUID opportunityId = uuid(args, "opportunityId");
        validateOwnedLinks(ctx.companyId(), contactId, opportunityId);

        Map<String, Object> parameters = newMap();
        put(parameters, "contactId", contactId);
        put(parameters, "opportunityId", opportunityId);
        put(parameters, "type", type.name());
        put(parameters, "subject", subject);
        put(parameters, "description", text(args, "description"));
        put(parameters, "activityAt", dateTime(args, "activityAt"));

        String description = "Registrar atividade (" + type.name().toLowerCase()
                + "): " + subject;
        AiActionResponse proposal = actionService.propose(
                ctx.companyId(), ctx.userId(), ctx.conversationId(), NAME, "ACTIVITY", null,
                parameters, description);
        return AiToolResult.ok(NAME, proposal);
    }

    private void validateOwnedLinks(UUID companyId, UUID contactId, UUID opportunityId) {
        if (contactId != null) {
            var contact = contactRepository.findById(contactId)
                    .orElseThrow(() -> new ContactNotFoundException(contactId));
            if (!contact.getCompanyId().equals(companyId) || !contact.isActive()) {
                throw new ContactNotFoundException(contactId);
            }
        }
        if (opportunityId != null) {
            var opportunity = opportunityRepository.findById(opportunityId)
                    .orElseThrow(() -> new OpportunityNotFoundException(opportunityId));
            if (!opportunity.getCompanyId().equals(companyId)) {
                throw new OpportunityNotFoundException(opportunityId);
            }
        }
    }
}