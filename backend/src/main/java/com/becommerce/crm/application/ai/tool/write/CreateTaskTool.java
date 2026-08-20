package com.becommerce.crm.application.ai.tool.write;

import com.becommerce.crm.application.ai.action.AiActionService;
import com.becommerce.crm.application.ai.dto.AiActionResponse;
import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.application.ai.tool.AiTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.contact.port.output.ContactRepository;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
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
 * Write tool {@code create_task} (AI-05). Nao cria a tarefa: valida os
 * argumentos e a propriedade dos vinculos (contact/opportunity da MESMA empresa
 * ativa), monta uma descricao amigavel e PERSISTE uma proposta
 * ({@code AiActionService#propose}) com os parametros tipados. A escrita real
 * so ocorre apos confirmacao explicita do usuario. Requer {@code task:create}.
 */
@Component
public class CreateTaskTool implements AiTool {

    private static final String NAME = "create_task";

    private final AiActionService actionService;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;

    public CreateTaskTool(@Lazy AiActionService actionService,
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
        return "Propoe a criacao de uma tarefa/follow-up. Nao executa a criacao: cria uma proposta "
                + "que o usuario deve confirmar antes de ser efetivada. Use quando o usuario pedir "
                + "para criar uma tarefa ou marcar um follow-up.";
    }

    @Override
    public String requiredPermission() {
        return "task:create";
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
                "title", Map.of("type", "string", "description", "Titulo da tarefa (obrigatorio, max 200)"),
                "description", Map.of("type", "string"),
                "contactId", Map.of("type", "string", "format", "uuid"),
                "opportunityId", Map.of("type", "string", "format", "uuid"),
                "assigneeId", Map.of("type", "string", "format", "uuid"),
                "dueAt", Map.of("type", "string", "format", "date-time"),
                "priority", Map.of("type", "string", "enum", List.of("LOW", "MEDIUM", "HIGH"))));
        schema.put("required", List.of("title"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        String title = text(args, "title");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("O titulo da tarefa e obrigatorio.");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("O titulo deve ter no maximo 200 caracteres.");
        }
        UUID contactId = uuid(args, "contactId");
        UUID opportunityId = uuid(args, "opportunityId");
        validateOwnedLinks(ctx.companyId(), contactId, opportunityId);

        Map<String, Object> parameters = newMap();
        put(parameters, "contactId", contactId);
        put(parameters, "opportunityId", opportunityId);
        put(parameters, "title", title);
        put(parameters, "description", text(args, "description"));
        put(parameters, "assigneeId", uuid(args, "assigneeId"));
        put(parameters, "dueAt", dateTime(args, "dueAt"));
        put(parameters, "priority", enumValue(args, "priority",
                com.becommerce.crm.domain.task.TaskPriority.class));

        String description = buildDescription(title, contactId, opportunityId);
        AiActionResponse proposal = actionService.propose(
                ctx.companyId(), ctx.userId(), ctx.conversationId(), NAME, "TASK", null,
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

    private String buildDescription(String title, UUID contactId, UUID opportunityId) {
        StringBuilder sb = new StringBuilder("Criar tarefa: ").append(title);
        if (contactId != null) {
            sb.append(" (contato vinculado)");
        }
        if (opportunityId != null) {
            sb.append(" (oportunidade vinculada)");
        }
        return sb.toString();
    }
}