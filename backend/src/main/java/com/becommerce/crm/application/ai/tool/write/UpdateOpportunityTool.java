package com.becommerce.crm.application.ai.tool.write;

import com.becommerce.crm.application.ai.action.AiActionService;
import com.becommerce.crm.application.ai.dto.AiActionResponse;
import com.becommerce.crm.application.ai.port.output.AiProvider;
import com.becommerce.crm.application.ai.tool.AiTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.pipeline.port.output.OpportunityRepository;
import com.becommerce.crm.domain.pipeline.exception.OpportunityNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.becommerce.crm.application.ai.tool.write.ToolArgument.dateTime;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.decimal;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.newMap;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.put;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.text;
import static com.becommerce.crm.application.ai.tool.write.ToolArgument.uuid;

/**
 * Write tool {@code update_opportunity} (AI-05). Nao atualiza a oportunidade:
 * valida os argumentos e a propriedade (oportunidade da MESMA empresa ativa),
 * monta uma descricao amigavel e PERSISTE uma proposta com os parametros
 * tipados. A escrita real so ocorre apos confirmacao explicita do usuario.
 *
 * <p>Somente campos suportados por {@code Opportunity#update} sao expostos:
 * {@code title}, {@code value}, {@code assignedTo}, {@code expectedCloseDate} e
 * {@code notes}. Mudancas de status/etapa nao sao feitas por aqui (fluxos
 * dedicados). Requer {@code opportunity:update}.</p>
 */
@Component
public class UpdateOpportunityTool implements AiTool {

    private static final String NAME = "update_opportunity";

    private final AiActionService actionService;
    private final OpportunityRepository opportunityRepository;

    public UpdateOpportunityTool(@Lazy AiActionService actionService,
                                 OpportunityRepository opportunityRepository) {
        this.actionService = actionService;
        this.opportunityRepository = opportunityRepository;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String description() {
        return "Propoe a atualizacao de uma oportunidade (titulo, valor, responsavel, previsao de "
                + "fechamento ou notas). Nao executa a atualizacao: cria uma proposta que o usuario "
                + "deve confirmar antes de ser efetivada. Nao altera status/etapa. Use quando o "
                + "usuario pedir para editar uma oportunidade.";
    }

    @Override
    public String requiredPermission() {
        return "opportunity:update";
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
                "opportunityId", Map.of("type", "string", "format", "uuid",
                        "description", "Id da oportunidade (obrigatorio)"),
                "title", Map.of("type", "string", "description", "Novo titulo (max 200)"),
                "value", Map.of("type", "number", "description", "Novo valor (maior que zero)"),
                "assignedTo", Map.of("type", "string", "format", "uuid"),
                "expectedCloseDate", Map.of("type", "string", "format", "date-time"),
                "notes", Map.of("type", "string", "description", "Notas (max 1000)")));
        schema.put("required", List.of("opportunityId"));
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name(), description(), inputSchema());
    }

    @Override
    public AiToolResult execute(AiToolContext ctx, Map<String, Object> args) {
        UUID opportunityId = uuid(args, "opportunityId");
        if (opportunityId == null) {
            throw new IllegalArgumentException("O id da oportunidade e obrigatorio.");
        }
        var opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new OpportunityNotFoundException(opportunityId));
        if (!opportunity.getCompanyId().equals(ctx.companyId())) {
            throw new OpportunityNotFoundException(opportunityId);
        }

        Map<String, Object> parameters = newMap();
        put(parameters, "opportunityId", opportunityId);
        put(parameters, "title", text(args, "title"));
        put(parameters, "value", decimal(args, "value"));
        put(parameters, "assignedTo", uuid(args, "assignedTo"));
        put(parameters, "expectedCloseDate", dateTime(args, "expectedCloseDate"));
        put(parameters, "notes", text(args, "notes"));

        String description = "Atualizar oportunidade: "
                + (text(args, "title") != null ? text(args, "title") : opportunity.getTitle());
        AiActionResponse proposal = actionService.propose(
                ctx.companyId(), ctx.userId(), ctx.conversationId(), NAME, "OPPORTUNITY",
                opportunityId, parameters, description);
        return AiToolResult.ok(NAME, proposal);
    }
}