package com.becommerce.crm.automation.ai.application.dto;

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
 *
 * <p>V088: blocos {@code identity} e {@code behavior}, e as flags de conversação
 * {@code memoryEnabled}/{@code humanTransferEnabled}. Todos opcionais — clientes
 * antigos (só campos planos) continuam funcionando. {@code systemPrompt} é o
 * prompt LEGADO, usado apenas enquanto identidade e comportamento estão vazios.</p>
 */
public record AgentConfigRequest(
        @NotNull Boolean aiEnabled,
        @NotNull Boolean allowAutoReply,
        @Size(max = 4000) String systemPrompt,
        @Size(max = 120) String model,
        @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
        @Positive Integer maxTokens,
        @NotNull @Min(0) @Max(1440) Integer cooldownMinutes,
        @NotNull @Min(1) @Max(5000) Integer maxChars,
        /** NEVER | MIRROR | ALWAYS — opcional (nulo mantém o atual). */
        @jakarta.validation.constraints.Pattern(regexp = "NEVER|MIRROR|ALWAYS") String voiceReplyMode,
        /** V088 — Identidade (nulo mantém a atual). */
        @jakarta.validation.Valid AgentIdentityDto identity,
        /** V088 — Comportamento (nulo mantém o atual). */
        @jakarta.validation.Valid AgentBehaviorDto behavior,
        /** V088 — Conversação → memória habilitada (nulo mantém). */
        Boolean memoryEnabled,
        /** V088 — Ferramenta de transferência humana habilitada (nulo mantém). */
        Boolean humanTransferEnabled
) {
    public AgentConfigRequest(Boolean aiEnabled, Boolean allowAutoReply, String systemPrompt, String model,
                              Double temperature, Integer maxTokens, Integer cooldownMinutes, Integer maxChars) {
        this(aiEnabled, allowAutoReply, systemPrompt, model, temperature, maxTokens, cooldownMinutes, maxChars, null);
    }

    public AgentConfigRequest(Boolean aiEnabled, Boolean allowAutoReply, String systemPrompt, String model,
                              Double temperature, Integer maxTokens, Integer cooldownMinutes, Integer maxChars,
                              String voiceReplyMode) {
        this(aiEnabled, allowAutoReply, systemPrompt, model, temperature, maxTokens, cooldownMinutes, maxChars,
                voiceReplyMode, null, null, null, null);
    }
}