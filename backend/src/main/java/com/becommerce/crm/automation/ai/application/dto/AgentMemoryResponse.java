package com.becommerce.crm.automation.ai.application.dto;

import com.becommerce.crm.automation.ai.domain.AgentMemory;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgentMemoryResponse(
        UUID id,
        UUID contactId,
        String type,
        String content,
        int importance,
        String source,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AgentMemoryResponse from(AgentMemory m) {
        return new AgentMemoryResponse(m.getId(), m.getContactId(), m.getType().name(), m.getContent(),
                m.getImportance(), m.getSource().name(), m.getExpiresAt(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
