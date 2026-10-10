package com.becommerce.crm.masterdata.anamnesis.application.dto;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisComplementTrigger;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestion;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;

import java.util.List;
import java.util.UUID;

public record AnamnesisQuestionResponse(
        UUID id,
        String text,
        AnamnesisQuestionType type,
        boolean required,
        boolean highlight,
        boolean allowComplement,
        String complementLabel,
        AnamnesisComplementTrigger complementTrigger,
        String complementOptionValue,
        int sortOrder,
        List<AnamnesisOptionResponse> options
) {
    public static AnamnesisQuestionResponse from(AnamnesisQuestion q, List<AnamnesisOptionResponse> options) {
        return new AnamnesisQuestionResponse(q.getId(), q.getText(), q.getType(), q.isRequired(),
                q.isHighlight(), q.isAllowComplement(), q.getComplementLabel(), q.getComplementTrigger(),
                q.getComplementOptionValue(), q.getSortOrder(), options);
    }
}
