package com.becommerce.crm.masterdata.anamnesis.application.dto;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionOption;

import java.util.UUID;

public record AnamnesisOptionResponse(
        UUID id,
        String label,
        String value,
        int sortOrder
) {
    public static AnamnesisOptionResponse from(AnamnesisQuestionOption o) {
        return new AnamnesisOptionResponse(o.getId(), o.getLabel(), o.getValue(), o.getSortOrder());
    }
}
