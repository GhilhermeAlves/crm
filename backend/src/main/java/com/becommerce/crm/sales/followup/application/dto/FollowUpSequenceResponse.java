package com.becommerce.crm.sales.followup.application.dto;

import com.becommerce.crm.sales.followup.domain.FollowUpSequence;
import com.becommerce.crm.sales.followup.domain.FollowUpSequenceStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/** Resposta de {@code FollowUpSequence} — nunca expõe companyId. */
public record FollowUpSequenceResponse(
        UUID id,
        String name,
        String description,
        FollowUpSequenceStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FollowUpSequenceResponse from(FollowUpSequence s) {
        return new FollowUpSequenceResponse(s.getId(), s.getName(), s.getDescription(),
                s.getStatus(), s.getCreatedAt(), s.getUpdatedAt());
    }
}