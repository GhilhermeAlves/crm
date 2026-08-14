package com.becommerce.crm.application.pipeline.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Reordenação dos estágios de um pipeline (Stages.md). A lista deve conter
 * todos os {@code id} de estágio do pipeline na nova ordem desejada.
 */
public record ReorderStagesRequest(
        @NotNull(message = "A lista de estágios é obrigatória.")
        @Size(min = 2, max = 15, message = "Um pipeline deve ter entre 2 e 15 estágios.")
        List<StagedItem> stages
) {
    public record StagedItem(
            @NotNull(message = "Id do estágio é obrigatório.") java.util.UUID id) {}
}
