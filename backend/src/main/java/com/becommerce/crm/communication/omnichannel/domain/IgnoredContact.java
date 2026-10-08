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

    /**
     * O mesmo celular brasileiro com e sem o nono dígito: o WhatsApp entrega
     * muitos números antigos sem o 9 (55 DD 8 dígitos), enquanto as pessoas
     * digitam com ele (55 DD 9 8 dígitos). Outros números voltam só como vieram.
     */
    public static java.util.List<String> phoneVariants(String phone) {
        String p = normalizePhone(phone);
        if (p.matches("55\\d{2}9\\d{8}")) {
            return java.util.List.of(p, p.substring(0, 4) + p.substring(5));
        }
        if (p.matches("55\\d{2}[6-9]\\d{7}")) {
            return java.util.List.of(p, p.substring(0, 4) + "9" + p.substring(4));
        }
        return java.util.List.of(p);
    }
}
