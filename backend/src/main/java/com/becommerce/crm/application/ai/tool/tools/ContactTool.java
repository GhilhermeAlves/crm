package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * {@code get_contact} (AI-03): obtém os dados de um contato por id. Reutiliza
 * {@link ContactUseCase#getById}. Exige {@code contact:read}.
 */
@Component
public class ContactTool extends AbstractAiReadTool {

    private static final String NAME = "get_contact";

    private final ContactUseCase contactUseCase;

    public ContactTool(ContactUseCase contactUseCase) {
        super(NAME, "Obtém os dados de um contato específico.",
                "contact:read",
                Map.of("contactId", stringProp("ID do contato", true)));
        this.contactUseCase = contactUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        UUID contactId = uuid(arguments, "contactId");
        if (contactId == null) {
            return AiToolResult.fail(NAME, "Parâmetro obrigatório ausente: contactId");
        }
        var contact = contactUseCase.getById(ctx.companyId(), contactId);
        return AiToolResult.ok(NAME, contact);
    }
}