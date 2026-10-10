package com.becommerce.crm.masterdata.anamnesis.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Criação de um modelo vazio (a estrutura é enviada depois em {@code update}). */
public record CreateAnamnesisModelRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 4000) String description
) {
}
