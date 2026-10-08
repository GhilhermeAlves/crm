package com.becommerce.crm.automation.ai.application.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/** Comportamento do agente na API: objetivo, tom e listas de regras/instruções. */
public record AgentBehaviorDto(
        @Size(max = 2000) String objective,
        @Size(max = 500) String tone,
        @Size(max = 30) List<@Size(max = 500) String> rules,
        @Size(max = 30) List<@Size(max = 500) String> instructions
) {
}
