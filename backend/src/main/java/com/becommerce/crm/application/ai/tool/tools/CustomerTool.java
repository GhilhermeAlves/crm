package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * {@code get_customer} (AI-03): obtém os dados de um cliente (contato) por id.
 * Reutiliza {@link ContactUseCase#getById}. Exige {@code contact:read}.
 */
@Component
public class CustomerTool extends AbstractAiReadTool {

    private static final String NAME = "get_customer";

    private final ContactUseCase contactUseCase;

    public CustomerTool(ContactUseCase contactUseCase) {
        super(NAME, "Obtém os dados de um cliente (contato) específico.",
                "contact:read",
                Map.of("customerId", stringProp("ID do cliente", true)));
        this.contactUseCase = contactUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        UUID customerId = uuid(arguments, "customerId");
        if (customerId == null) {
            return AiToolResult.fail(NAME, "Parâmetro obrigatório ausente: customerId");
        }
        var customer = contactUseCase.getById(ctx.companyId(), customerId);
        return AiToolResult.ok(NAME, customer);
    }
}