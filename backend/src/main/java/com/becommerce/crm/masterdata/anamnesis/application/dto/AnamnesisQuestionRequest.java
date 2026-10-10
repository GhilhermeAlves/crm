package com.becommerce.crm.masterdata.anamnesis.application.dto;

import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisComplementTrigger;
import com.becommerce.crm.masterdata.anamnesis.domain.AnamnesisQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Pergunta de uma seção. Campo complementar é configurado por
 * {@code allowComplement} + {@code complementTrigger} (YES/NO/ALWAYS/OPTION).
 */
public record AnamnesisQuestionRequest(
        UUID id,
        @NotBlank @Size(max = 4000) String text,
        @NotNull AnamnesisQuestionType type,
        Boolean required,
        Boolean highlight,
        Boolean allowComplement,
        @Size(max = 255) String complementLabel,
        AnamnesisComplementTrigger complementTrigger,
        @Size(max = 255) String complementOptionValue,
        List<AnamnesisOptionRequest> options
) {
}
