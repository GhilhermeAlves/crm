package com.becommerce.crm.automation.ai.domain;

import java.util.List;
import java.util.Objects;

/**
 * Comportamento do agente: COMO ele age (objetivo, tom de voz, regras e
 * instruções). Regras e instruções são listas ordenadas — cada item é uma
 * diretriz independente, o que evita o "prompt gigante".
 */
public record AgentBehavior(String objective, String tone, List<String> rules, List<String> instructions) {

    public static final AgentBehavior EMPTY = new AgentBehavior(null, null, List.of(), List.of());

    public AgentBehavior {
        objective = AgentIdentity.blankToNull(objective);
        tone = AgentIdentity.blankToNull(tone);
        rules = clean(rules);
        instructions = clean(instructions);
    }

    public boolean isEmpty() {
        return objective == null && tone == null && rules.isEmpty() && instructions.isEmpty();
    }

    private static List<String> clean(List<String> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
