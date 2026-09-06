package com.becommerce.crm.application.ai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Corpo de atualização do agente de IA (Sprint 3 - AgentConfig Administrativo).
 *
 * <p>Validação espelhando as constraints reais do banco (V070/V071):
 * <ul>
 *   <li>{@code aiEnabled}/{@code allowAutoReply} — obrigatórios (BOOLEAN NOT NULL);</li>
 *   <li>{@code systemPrompt} — opcional, limite de 4000 caracteres;</li>
 *   <li>{@code model} — opcional, VARCHAR(120);</li>
 *   <li>{@code temperature} — opcional (NULL = default do provider), 0.0 a 2.0
 *       (CHECK da V071);</li>
 *   <li>{@code maxTokens} — opcional (NULL = default do provider), &gt; 0 (CHECK da V071);</li>
 *   <li>{@code cooldownMinutes} — 0 a 1440 (CHECK da V070);</li>
 *   <li>{@code maxChars} — 1 a 5000 (CHECK da V070).</li>
 * </ul>
 */
public record AgentConfigRequest(
        @NotNull Boolean aiEnabled,
        @NotNull Boolean allowAutoReply,
        @Size(max = 4000) String systemPrompt,
        @Size(max = 120) String model,
        @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
        @Positive Integer maxTokens,
        @NotNull @Min(0) @Max(1440) Integer cooldownMinutes,
        @NotNull @Min(1) @Max(5000) Integer maxChars
) {
}