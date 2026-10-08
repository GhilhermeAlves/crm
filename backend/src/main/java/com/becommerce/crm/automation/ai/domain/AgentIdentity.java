package com.becommerce.crm.automation.ai.domain;

/**
 * Identidade do agente: QUEM ele é (nome, descrição curta e persona). Parte da
 * configuração permanente — nunca contém dados de clínica, paciente ou memória,
 * que são carregados dinamicamente pelo {@code AgentContextBuilder}.
 */
public record AgentIdentity(String name, String description, String persona) {

    public static final AgentIdentity EMPTY = new AgentIdentity(null, null, null);

    public AgentIdentity {
        name = blankToNull(name);
        description = blankToNull(description);
        persona = blankToNull(persona);
    }

    public boolean isEmpty() {
        return name == null && description == null && persona == null;
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
