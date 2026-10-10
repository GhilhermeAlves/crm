package com.becommerce.crm.masterdata.anamnesis.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** Seção de um modelo. {@code professional=true} indica preenchimento do cirurgião-dentista. */
public record AnamnesisSectionRequest(
        UUID id,
        @NotBlank @Size(max = 160) String title,
        Boolean professional,
        @Valid List<AnamnesisQuestionRequest> questions
) {
}
