package com.becommerce.crm.automation.ai.domain;

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
 *
 * <p>V088 — camadas do agente: {@link AgentIdentity} (quem é),
 * {@link AgentBehavior} (como age), modelo ({@code model/temperature/maxTokens})
 * e conversação ({@code cooldown/maxChars/voz/memória/transferência humana}).
 * {@code systemPrompt} passa a ser o PROMPT LEGADO: só é usado enquanto não houver
 * perfil estruturado. Dados de clínica, paciente, memória e histórico NUNCA ficam
 * aqui — são montados em tempo de execução pelo {@code AgentContextBuilder}.</p>
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
    /** Não entra nos construtores (compatibilidade); padrão MIRROR, como na V086. */
    private VoiceReplyMode voiceReplyMode = VoiceReplyMode.MIRROR;
    /** V088 — fora dos construtores (compatibilidade), como {@code voiceReplyMode}. */
    private AgentIdentity identity = AgentIdentity.EMPTY;
    private AgentBehavior behavior = AgentBehavior.EMPTY;
    private boolean memoryEnabled;
    private boolean humanTransferEnabled;

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
                temperature, maxTokens, cooldownMinutes, maxChars, createdAt, LocalDateTime.now())
                .withVoiceReplyMode(voiceReplyMode)
                .withProfile(identity, behavior, memoryEnabled, humanTransferEnabled);
    }

    /** Auto-resposta habilitada apenas se {@code aiEnabled} E {@code allowAutoReply}. */
    public VoiceReplyMode getVoiceReplyMode() {
        return voiceReplyMode;
    }

    /** Define o modo de resposta em voz (nulo mantém o atual). Retorna a própria instância. */
    public AgentConfig withVoiceReplyMode(VoiceReplyMode mode) {
        if (mode != null) {
            this.voiceReplyMode = mode;
        }
        return this;
    }

    /**
     * Define identidade, comportamento e capacidades de conversação (V088).
     * Nulos mantêm o valor atual. Retorna a própria instância.
     */
    public AgentConfig withProfile(AgentIdentity identity, AgentBehavior behavior,
                                   Boolean memoryEnabled, Boolean humanTransferEnabled) {
        if (identity != null) {
            this.identity = identity;
        }
        if (behavior != null) {
            this.behavior = behavior;
        }
        if (memoryEnabled != null) {
            this.memoryEnabled = memoryEnabled;
        }
        if (humanTransferEnabled != null) {
            this.humanTransferEnabled = humanTransferEnabled;
        }
        return this;
    }

    /**
     * {@code true} quando a configuração estruturada (identidade/comportamento)
     * está preenchida — nesse caso o prompt legado deixa de ser usado.
     */
    public boolean hasStructuredProfile() {
        return !identity.isEmpty() || !behavior.isEmpty();
    }

    /** Prompt legado (coluna {@code system_prompt}) — usado só sem perfil estruturado. */
    public String getLegacyPrompt() {
        return systemPrompt;
    }

    public AgentIdentity getIdentity() { return identity; }
    public AgentBehavior getBehavior() { return behavior; }
    public boolean isMemoryEnabled() { return memoryEnabled; }
    public boolean isHumanTransferEnabled() { return humanTransferEnabled; }

    public boolean canAutoReply() {
        return aiEnabled && allowAutoReply;
    }

    /**
     * Há instruções utilizáveis — perfil estruturado OU prompt legado não-branco
     * (requisito de não hardcode).
     */
    public boolean hasUsablePrompt() {
        return hasStructuredProfile() || (systemPrompt != null && !systemPrompt.isBlank());
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
                && Objects.equals(maxTokens, other.maxTokens)
                && memoryEnabled == other.memoryEnabled
                && humanTransferEnabled == other.humanTransferEnabled
                && Objects.equals(identity, other.identity)
                && Objects.equals(behavior, other.behavior);
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyId, aiEnabled, allowAutoReply, systemPrompt, model,
                temperature, maxTokens, cooldownMinutes, maxChars, identity, behavior);
    }
}