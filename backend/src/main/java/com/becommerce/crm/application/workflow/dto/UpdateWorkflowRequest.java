package com.becommerce.crm.application.workflow.dto;

import com.becommerce.crm.domain.workflow.TriggerEvent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateWorkflowRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String name,
        String description,
        @NotNull(message = "O trigger é obrigatório")
        TriggerEvent trigger,
        @Valid List<WorkflowConditionRequest> conditions,
        @NotEmpty(message = "Pelo menos uma ação é obrigatória")
        @Valid List<WorkflowActionRequest> actions
) {
}
