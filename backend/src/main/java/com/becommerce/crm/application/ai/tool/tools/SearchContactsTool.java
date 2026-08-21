package com.becommerce.crm.application.ai.tool.tools;

import com.becommerce.crm.application.ai.tool.AbstractAiReadTool;
import com.becommerce.crm.application.ai.tool.AiToolContext;
import com.becommerce.crm.application.ai.tool.AiToolResult;
import com.becommerce.crm.application.contact.port.input.ContactUseCase;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * {@code search_contacts} (AI-03): busca contatos por nome/e-mail/telefone,
 * limitada. Reutiliza {@link ContactUseCase#search}. Exige {@code contact:read}.
 */
@Component
public class SearchContactsTool extends AbstractAiReadTool {

    private static final String NAME = "search_contacts";

    private final ContactUseCase contactUseCase;

    public SearchContactsTool(ContactUseCase contactUseCase) {
        super(NAME, "Busca contatos por nome, e-mail ou telefone (lista limitada).",
                "contact:read",
                Map.of("query", stringProp("Termo de busca (nome/e-mail/telefone)"),
                        "limit", integerProp("Quantidade máxima de resultados (máx. 50)")));
        this.contactUseCase = contactUseCase;
    }

    @Override
    protected AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments) {
        String query = string(arguments, "query");
        int limit = clampLimit(integer(arguments, "limit"));
        var contacts = contactUseCase.search(ctx.companyId(), query, limit);
        return AiToolResult.ok(NAME, contacts);
    }
}