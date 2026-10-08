package com.becommerce.crm.automation.ai.domain;

import java.util.Locale;

/**
 * Tipos de memória do agente. Memória é informação duradoura sobre o contato —
 * nunca histórico de mensagens nem dado transacional (agenda/status vêm do CRM).
 */
public enum MemoryType {
    /** Ex.: prefere atendimento pela manhã. */
    PREFERENCE,
    /** Ex.: já realizou avaliação odontológica. */
    FACT,
    /** Ex.: demonstrou interesse em clareamento. */
    PROFILE,
    /** Ex.: aguardando retorno sobre orçamento. */
    CONTEXTUAL;

    /** Converte sem lançar; valor desconhecido → {@code null}. */
    public static MemoryType parse(String value) {
        if (value == null) {
            return null;
        }
        try {
            return MemoryType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
