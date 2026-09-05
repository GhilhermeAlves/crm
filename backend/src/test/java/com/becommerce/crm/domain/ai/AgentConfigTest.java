package com.becommerce.crm.domain.ai;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regras do {@link AgentConfig} (Sprint 1 + Sprint 2): safe defaults, prompt
 * utilizável e campos opcionais de geração (model/temperature/maxTokens nulos →
 * default do provider).
 */
class AgentConfigTest {

    private final UUID companyId = UUID.randomUUID();

    @Test
    void aiDisabled_shouldNeverAutoReply() {
        AgentConfig config = config(true, false, "prompt", 60, 1000);
        assertTrue(config.isAiEnabled());
        assertFalse(config.isAllowAutoReply());
        assertFalse(config.canAutoReply());
    }

    @Test
    void autoReplyDisabled_shouldNeverAutoReply() {
        AgentConfig config = config(false, true, "prompt", 60, 1000);
        assertFalse(config.isAiEnabled());
        assertTrue(config.isAllowAutoReply());
        assertFalse(config.canAutoReply());
    }

    @Test
    void bothDisabled_shouldNeverAutoReply() {
        AgentConfig config = config(false, false, "prompt", 60, 1000);
        assertFalse(config.canAutoReply());
    }

    @Test
    void blankPrompt_shouldNotBeUsable() {
        AgentConfig nullPrompt = config(true, true, null, 60, 1000);
        AgentConfig blankPrompt = config(true, true, "   ", 60, 1000);
        AgentConfig emptyPrompt = config(true, true, "", 60, 1000);
        assertFalse(nullPrompt.hasUsablePrompt());
        assertFalse(blankPrompt.hasUsablePrompt());
        assertFalse(emptyPrompt.hasUsablePrompt());
        // Guarda combinada usada pelo processor: auto-reply só se habilitada E prompt utilizável.
        assertFalse(nullPrompt.canAutoReply() && nullPrompt.hasUsablePrompt());
        assertFalse(blankPrompt.canAutoReply() && blankPrompt.hasUsablePrompt());
        assertFalse(emptyPrompt.canAutoReply() && emptyPrompt.hasUsablePrompt());
    }

    @Test
    void validPrompt_whenEnabled_shouldAutoReply() {
        AgentConfig config = config(true, true, "Você responde como Léo.", 60, 1000);
        assertTrue(config.hasUsablePrompt());
        assertTrue(config.canAutoReply());
    }

    @Test
    void generationFields_preservedThroughCreate() {
        AgentConfig config = AgentConfig.create(companyId, true, true,
                "prompt", "gpt-4o", 0.7, 300, 45, 900);

        assertEquals("gpt-4o", config.getModel());
        assertEquals(0.7, config.getTemperature());
        assertEquals(300, config.getMaxTokens());
        assertEquals(companyId, config.getCompanyId());
        assertEquals(45, config.getCooldownMinutes());
        assertEquals(900, config.getMaxChars());
        assertTrue(config.canAutoReply());
        assertTrue(config.hasUsablePrompt());
    }

    @Test
    void generationFields_null_meansProviderDefaults() {
        AgentConfig config = config(true, true, "prompt", 60, 1000);
        assertNull(config.getModel());
        assertNull(config.getTemperature());
        assertNull(config.getMaxTokens());
    }

    @Test
    void reconstitute_shouldPreserveEveryField() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        AgentConfig config = AgentConfig.reconstitute(id, companyId, true, true, "prompt",
                "gpt-4o-mini", 0.3, 500, 30, 800, now, now);

        assertEquals(id, config.getId());
        assertEquals(companyId, config.getCompanyId());
        assertTrue(config.isAiEnabled());
        assertTrue(config.isAllowAutoReply());
        assertEquals("prompt", config.getSystemPrompt());
        assertEquals("gpt-4o-mini", config.getModel());
        assertEquals(0.3, config.getTemperature());
        assertEquals(500, config.getMaxTokens());
        assertEquals(30, config.getCooldownMinutes());
        assertEquals(800, config.getMaxChars());
        assertEquals(now, config.getCreatedAt());
        assertEquals(now, config.getUpdatedAt());
        assertEquals(config, AgentConfig.reconstitute(id, companyId, true, true, "prompt",
                "gpt-4o-mini", 0.3, 500, 30, 800, now, now));
    }

    private AgentConfig config(boolean aiEnabled, boolean allowAutoReply, String prompt,
                               int cooldownMinutes, int maxChars) {
        return AgentConfig.create(companyId, aiEnabled, allowAutoReply, prompt,
                null, null, null, cooldownMinutes, maxChars);
    }
}