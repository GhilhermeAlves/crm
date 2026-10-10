package com.becommerce.crm.masterdata.anamnesis.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Opção de resposta de uma pergunta de seleção. {@code id} opcional (gerado se ausente). */
public record AnamnesisOptionRequest(
        UUID id,
        @NotBlank @Size(max = 255) String label,
        @Size(max = 255) String value
) {
}
