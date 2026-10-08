package com.becommerce.crm.automation.ai.application.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Configuração do agente de IA no formato de resposta (Sprint 3).
 *
 * <p>NUNCA expõe segredos/API keys — apenas os parâmetros operacionais usados
 * pelo {@code WhatsAppInboundAutoReplyProcessor}. Quando o registro ainda não
 * existe (safe default), {@code id} e {@code updatedAt} são {@code null} e os
 * parâmetros retornam os defaults de fábrica ({@code aiEnabled=false},
 * {@code allowAutoReply=false}, {@code cooldownMinutes=60}, {@code maxChars=1000}).
 *
 * <p>V088 — além dos campos planos (compatibilidade), expõe a arquitetura do
 * agente em blocos: {@code identity}, {@code behavior}, {@code model},
 * {@code conversation} (com {@code memory}) e {@code tools}.
 * {@code usesLegacyPrompt} indica que o runtime ainda usa {@code systemPrompt}.</p>
 */
public record AgentConfigResponse(
        UUID id,
        boolean aiEnabled,
        boolean allowAutoReply,
        String systemPrompt,
        String model,
        Double temperature,
        Integer maxTokens,
        int cooldownMinutes,
        int maxChars,
        LocalDateTime updatedAt,
        String voiceReplyMode,
        AgentIdentityDto identity,
        AgentBehaviorDto behavior,
        ModelSection modelConfig,
        ConversationSection conversation,
        ToolsSection tools,
        boolean usesLegacyPrompt
) {
    public record ModelSection(String model, Double temperature, Integer maxTokens) {
    }

    public record MemorySection(boolean enabled) {
    }

    public record ConversationSection(int cooldownMinutes, int maxChars, String voiceReplyMode, MemorySection memory) {
    }

    public record ToolsSection(boolean humanTransferEnabled) {
    }

    public AgentConfigResponse(UUID id, boolean aiEnabled, boolean allowAutoReply, String systemPrompt,
                               String model, Double temperature, Integer maxTokens, int cooldownMinutes,
                               int maxChars, LocalDateTime updatedAt, String voiceReplyMode) {
        this(id, aiEnabled, allowAutoReply, systemPrompt, model, temperature, maxTokens, cooldownMinutes,
                maxChars, updatedAt, voiceReplyMode,
                new AgentIdentityDto(null, null, null), new AgentBehaviorDto(null, null, List.of(), List.of()),
                new ModelSection(model, temperature, maxTokens),
                new ConversationSection(cooldownMinutes, maxChars, voiceReplyMode, new MemorySection(false)),
                new ToolsSection(false), true);
    }

    public AgentConfigResponse(UUID id, boolean aiEnabled, boolean allowAutoReply, String systemPrompt,
                               String model, Double temperature, Integer maxTokens, int cooldownMinutes,
                               int maxChars, LocalDateTime updatedAt) {
        this(id, aiEnabled, allowAutoReply, systemPrompt, model, temperature, maxTokens, cooldownMinutes,
                maxChars, updatedAt, "MIRROR");
    }
}
