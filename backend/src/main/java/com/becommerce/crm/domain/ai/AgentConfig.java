package com.becommerce.crm.domain.ai;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuração do agente de IA autônomo de uma empresa (Sprint 1 da portabilidade
 * Q7 → CRM). Correspondente à tabela {@code agent_config} (V070/V071), protegida
 * por RLS.
 *
 * <p>Regra de segurança (safe defaults): a AUSÊNCIA de config ou campos
 * desabilitados significam que NENHUMA auto-resposta será enviada. O prompt do
 * agente vem exclusivamente de {@link #getSystemPrompt()} — nunca é hardcoded.</p>
 *
 * <p>Sprint 2: campos opcionais de geração ({@code model}, {@code temperature},
 * {@code maxTokens}) — quando nulos, o provider usa o seu default (consistente
 * com a configuração de infraestrutura {@code app.ai.*}). Não existe "provider"
 * por empresa: o provider é selecionado em deploy via {@code app.ai.provider}
 * (arquitetura atual).</p>
 */
public class AgentConfig {

    private final UUID id;
    private final UUID companyId;
    private final boolean aiEnabled;
    private final boolean allowAutoReply;
    private final String systemPrompt;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;
    private final int cooldownMinutes;
    private final int maxChars;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AgentConfig(UUID id, UUID companyId, boolean aiEnabled, boolean allowAutoReply,
                        String systemPrompt, String model, Double temperature, Integer maxTokens,
                        int cooldownMinutes, int maxChars,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.aiEnabled = aiEnabled;
        this.allowAutoReply = allowAutoReply;
        this.systemPrompt = systemPrompt;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.cooldownMinutes = cooldownMinutes;
        this.maxChars = maxChars;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AgentConfig create(UUID companyId, boolean aiEnabled, boolean allowAutoReply,
                                     String systemPrompt, String model, Double temperature,
                                     Integer maxTokens, int cooldownMinutes, int maxChars) {
        LocalDateTime now = LocalDateTime.now();
        return new AgentConfig(UUID.randomUUID(), companyId, aiEnabled, allowAutoReply,
                systemPrompt, model, temperature, maxTokens, cooldownMinutes, maxChars, now, now);
    }

    public static AgentConfig reconstitute(UUID id, UUID companyId, boolean aiEnabled,
                                           boolean allowAutoReply, String systemPrompt,
                                           String model, Double temperature, Integer maxTokens,
                                           int cooldownMinutes, int maxChars,
                                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AgentConfig(id, companyId, aiEnabled, allowAutoReply, systemPrompt, model,
                temperature, maxTokens, cooldownMinutes, maxChars, createdAt, updatedAt);
    }

    /**
     * Retorna uma nova instância com os parâmetros atualizados, preservando
     * {@code id}, {@code companyId} e {@code createdAt} e renovando
     * {@code updatedAt} (Sprint 3 - AgentConfig Administrativo).
     */
    public AgentConfig withSettings(boolean aiEnabled, boolean allowAutoReply, String systemPrompt,
                                    String model, Double temperature, Integer maxTokens,
                                    int cooldownMinutes, int maxChars) {
        return new AgentConfig(id, companyId, aiEnabled, allowAutoReply, systemPrompt, model,
                temperature, maxTokens, cooldownMinutes, maxChars, createdAt, LocalDateTime.now());
    }

    /** Auto-resposta habilitada apenas se {@code aiEnabled} E {@code allowAutoReply}. */
    public boolean canAutoReply() {
        return aiEnabled && allowAutoReply;
    }

    /** Prompt utilizável (não-nulo e não-branco) — requisito de não hardcode. */
    public boolean hasUsablePrompt() {
        return systemPrompt != null && !systemPrompt.isBlank();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public boolean isAiEnabled() { return aiEnabled; }
    public boolean isAllowAutoReply() { return allowAutoReply; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getModel() { return model; }
    public Double getTemperature() { return temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public int getCooldownMinutes() { return cooldownMinutes; }
    public int getMaxChars() { return maxChars; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AgentConfig other)) return false;
        return aiEnabled == other.aiEnabled
                && allowAutoReply == other.allowAutoReply
                && cooldownMinutes == other.cooldownMinutes
                && maxChars == other.maxChars
                && Objects.equals(companyId, other.companyId)
                && Objects.equals(systemPrompt, other.systemPrompt)
                && Objects.equals(model, other.model)
                && Objects.equals(temperature, other.temperature)
                && Objects.equals(maxTokens, other.maxTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, aiEnabled, allowAutoReply, systemPrompt, model,
                temperature, maxTokens, cooldownMinutes, maxChars);
    }
}