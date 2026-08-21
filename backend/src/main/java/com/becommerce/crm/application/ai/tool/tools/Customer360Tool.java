package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.customer360.service.Customer360Service;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@code get_customer_360} (AI-03): visão consolidada (Customer 360) de um
 * cliente por id. Reutiliza {@link Customer360Service#build}. Exige
 * {@code contact:read}.
 */
@Component
public class Customer360Tool extends AbstractAiReadTool {

    private static final String NAME = "get_customer_360";

    private final Customer360Service customer360Service;

    public Customer360Tool(Customer360Service customer360Service) {
        super(NAME, "Obtém a visão completa (Customer 360) de um cliente: dados, "
                        + "oportunidades, tarefas, linha do tempo e próxima ação.",
                "contact:read",
                Map.of("customerId", stringProp("ID do cliente")), List.of("customerId"));
        this.customer360Service = customer360Service;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        UUID customerId = uuid(arguments, "customerId");
        if (customerId == null) {
            return AiToolResult.fail(NAME, "Parâmetro obrigatório ausente: customerId");
        }
        var view = customer360Service.build(ctx.companyId(), customerId);
        return AiToolResult.ok(NAME, view);
    }
}