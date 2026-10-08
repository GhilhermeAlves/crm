package com.becommerce.crm.automation.ai.application.agent.context;

import com.becommerce.crm.automation.ai.domain.AgentMemory;

import java.util.List;

/** Memórias relevantes recuperadas para esta interação (já limitadas). */
public record MemoryContext(boolean enabled, List<AgentMemory> memories) {

    public static final MemoryContext DISABLED = new MemoryContext(false, List.of());

    public MemoryContext {
        memories = memories == null ? List.of() : List.copyOf(memories);
    }
}
