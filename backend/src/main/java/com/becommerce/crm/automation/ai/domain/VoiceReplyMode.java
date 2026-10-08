package com.becommerce.crm.automation.ai.domain;

/** Quando o agente de WhatsApp responde com mensagem de voz. */
public enum VoiceReplyMode {
    /** Sempre texto. */
    NEVER,
    /** Voz só quando o paciente mandou áudio (espelha o paciente). */
    MIRROR,
    /** Sempre voz. */
    ALWAYS;

    public static VoiceReplyMode parseOrDefault(String value, VoiceReplyMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
