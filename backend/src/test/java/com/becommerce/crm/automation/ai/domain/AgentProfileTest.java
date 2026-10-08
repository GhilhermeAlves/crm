package com.becommerce.crm.automation.ai.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Identidade/comportamento estruturados (V088), compatibilidade com o prompt legado e memória. */
class AgentProfileTest {

    private AgentConfig legacy(String prompt) {
        return AgentConfig.reconstitute(UUID.randomUUID(), UUID.randomUUID(), true, true, prompt, null, null,
                null, 60, 1000, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void legacyAgent_withoutProfile_keepsUsingPrompt() {
        AgentConfig agent = legacy("Você é a recepcionista.");

        assertFalse(agent.hasStructuredProfile());
        assertTrue(agent.hasUsablePrompt());
        assertEquals("Você é a recepcionista.", agent.getLegacyPrompt());
    }

    @Test
    void structuredProfile_makesAgentUsableEvenWithoutLegacyPrompt() {
        AgentConfig agent = legacy(null).withProfile(new AgentIdentity("Ana Laura", null, "Você é Ana Laura."),
                null, null, null);

        assertTrue(agent.hasStructuredProfile());
        assertTrue(agent.hasUsablePrompt());
    }

    @Test
    void blankIdentityAndBehavior_areEmpty() {
        assertTrue(new AgentIdentity(" ", "", null).isEmpty());
        assertTrue(new AgentBehavior(" ", null, Arrays.asList(" ", null), List.of()).isEmpty());
        assertFalse(legacy(" ").hasUsablePrompt());
    }

    @Test
    void behavior_trimsAndDropsBlankItems() {
        AgentBehavior behavior = new AgentBehavior("Agendar", "Acolhedor",
                Arrays.asList("  Não inventar  ", "", null), List.of("Confirmar data"));

        assertEquals(List.of("Não inventar"), behavior.rules());
        assertEquals(List.of("Confirmar data"), behavior.instructions());
    }

    @Test
    void withSettings_preservesProfileVoiceAndCapabilities() {
        AgentConfig agent = legacy("p").withVoiceReplyMode(VoiceReplyMode.ALWAYS)
                .withProfile(new AgentIdentity("Ana", null, null), AgentBehavior.EMPTY, true, true);

        AgentConfig updated = agent.withSettings(true, false, "p2", null, null, null, 10, 500);

        assertEquals("Ana", updated.getIdentity().name());
        assertTrue(updated.isMemoryEnabled());
        assertTrue(updated.isHumanTransferEnabled());
        assertEquals(VoiceReplyMode.ALWAYS, updated.getVoiceReplyMode());
    }

    @Test
    void capabilities_defaultOff() {
        AgentConfig agent = legacy("p");
        assertFalse(agent.isMemoryEnabled());
        assertFalse(agent.isHumanTransferEnabled());
    }

    @Test
    void memory_validatesScopeContentAndImportance() {
        UUID company = UUID.randomUUID();
        UUID agent = UUID.randomUUID();
        UUID contact = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> AgentMemory.create(company, agent, null,
                MemoryType.FACT, "conteúdo", null, null, null, Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> AgentMemory.create(company, agent, contact,
                MemoryType.FACT, " ", null, null, null, Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> AgentMemory.create(company, agent, contact,
                MemoryType.FACT, "x".repeat(501), null, null, null, Map.of(), null));
        assertThrows(IllegalArgumentException.class, () -> AgentMemory.create(company, agent, contact,
                MemoryType.FACT, "conteúdo", 6, null, null, Map.of(), null));

        AgentMemory memory = AgentMemory.create(company, agent, contact, MemoryType.FACT, " conteúdo ", null,
                null, null, null, null);
        assertEquals("conteúdo", memory.getContent());
        assertEquals(AgentMemory.DEFAULT_IMPORTANCE, memory.getImportance());
        assertEquals(MemorySource.AGENT, memory.getSource());
        assertTrue(memory.belongsTo(company, contact));
        assertFalse(memory.belongsTo(UUID.randomUUID(), contact));
    }

    @Test
    void memoryType_parseIsLenient() {
        assertEquals(MemoryType.PREFERENCE, MemoryType.parse(" preference "));
        assertEquals(null, MemoryType.parse("qualquer"));
    }
}
