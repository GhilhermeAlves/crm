package com.becommerce.crm.automation.ai.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Criação/atualização manual de memória pela equipe. Em atualização,
 * {@code contactId} é ignorado e campos nulos mantêm o valor atual.
 */
public record AgentMemoryRequest(
        UUID contactId,
        @Pattern(regexp = "PREFERENCE|FACT|PROFILE|CONTEXTUAL") String type,
        @Size(max = 500) String content,
        @Min(1) @Max(5) Integer importance
) {
}
