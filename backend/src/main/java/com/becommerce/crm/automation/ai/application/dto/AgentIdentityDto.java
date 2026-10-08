package com.becommerce.crm.automation.ai.application.dto;

import jakarta.validation.constraints.Size;

/** Identidade do agente na API (limites espelham a V088). */
public record AgentIdentityDto(
        @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 6000) String persona
) {
}
