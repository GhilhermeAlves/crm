package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.domain.ai.AiRecordType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolver de contexto para {@code CUSTOMER} (AI-02). Resolve o Customer 360 do
 * contato em foco. Exige {@code contact:read}.
 */
@Component
public class CustomerContextResolver implements AiRecordContextResolver {

    public static final String PERMISSION = "contact:read";

    private final Customer360ContextBuilder builder;

    public CustomerContextResolver(Customer360ContextBuilder builder) {
        this.builder = builder;
    }

    @Override
    public AiRecordType type() {
        return AiRecordType.CUSTOMER;
    }

    @Override
    public String requiredPermission() {
        return PERMISSION;
    }

    @Override
    public String resolve(UUID companyId, UUID recordId) {
        return builder.build(companyId, recordId);
    }
}