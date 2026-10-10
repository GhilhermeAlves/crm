package com.becommerce.crm.masterdata.anamnesis.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Atualização completa de um modelo (nome/descrição + estrutura de seções). */
public record UpdateAnamnesisModelRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 4000) String description,
        @Valid List<AnamnesisSectionRequest> sections
) {
}
