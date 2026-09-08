package com.becommerce.crm.application.followup.dto;

import com.becommerce.crm.domain.followup.FollowUp;
import com.becommerce.crm.domain.followup.FollowUpAction;
import com.becommerce.crm.domain.followup.FollowUpCancellationReason;
import com.becommerce.crm.domain.followup.FollowUpStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Resposta de FollowUp — nunca expõe {@code companyId}. */
public record FollowUpResponse(
        UUID id,
        UUID conversationId,
        FollowUpStatus status,
        FollowUpAction actionType,
        String actionContent,
        LocalDateTime executeAt,
        int attempts,
        String lastError,
        String resultText,
        LocalDateTime cancelledAt,
        FollowUpCancellationReason cancelledReason,
        UUID sequenceId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FollowUpResponse from(FollowUp f) {
        return new FollowUpResponse(f.getId(), f.getConversationId(), f.getStatus(), f.getActionType(),
                f.getActionContent(), f.getExecuteAt(), f.getAttempts(), f.getLastError(), f.getResultText(),
                f.getCancelledAt(), f.getCancelledReason(), f.getSequenceId(), f.getCreatedAt(), f.getUpdatedAt());
    }
}