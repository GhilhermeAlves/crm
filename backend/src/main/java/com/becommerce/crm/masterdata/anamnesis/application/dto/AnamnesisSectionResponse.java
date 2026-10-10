package com.becommerce.crm.masterdata.anamnesis.application.dto;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisSection;

import java.util.List;
import java.util.UUID;

public record AnamnesisSectionResponse(
        UUID id,
        String title,
        boolean professional,
        int sortOrder,
        List<AnamnesisQuestionResponse> questions
) {
    public static AnamnesisSectionResponse from(AnamnesisSection s, List<AnamnesisQuestionResponse> questions) {
        return new AnamnesisSectionResponse(s.getId(), s.getTitle(), s.isProfessional(), s.getSortOrder(),
                questions);
    }
}
