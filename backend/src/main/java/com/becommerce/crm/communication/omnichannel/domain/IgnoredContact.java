package com.becommerce.crm.communication.omnichannel.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Número que a IA nunca responde e cujas mensagens não são gravadas (ex.:
 * família/amigos quando o WhatsApp do atendimento é o celular pessoal).
 */
public record IgnoredContact(UUID id, UUID companyId, String phone, String label, LocalDateTime createdAt) {

    /** Normaliza para só dígitos; telefones brasileiros sem DDI ganham 55. */
    public static String normalizePhone(String raw) {
        if (raw == null) {
            return "";
        }
        String digits = raw.replaceAll("\\D", "");
        return digits.length() == 10 || digits.length() == 11 ? "55" + digits : digits;
    }
}
