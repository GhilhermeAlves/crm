package com.becommerce.crm.automation.ai.application.memory;

import com.becommerce.crm.automation.ai.domain.AgentMemory;
import com.becommerce.crm.automation.ai.domain.MemoryType;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Seleção do que pode virar memória (regra obrigatória: nem toda mensagem é
 * memória). Determinística e conservadora; o modelo PROPÕE via ferramenta e
 * esta política decide. Rejeita:
 * <ul>
 *   <li>conteúdo curto demais ou longo demais;</li>
 *   <li>cumprimentos/conteúdo sem informação ("oi", "ok", "obrigado");</li>
 *   <li>dados transacionais que o CRM já controla (agendamentos com data/hora,
 *       status de consulta) — o CRM é a fonte de verdade;</li>
 *   <li>documentos pessoais (CPF/cartão) — LGPD.</li>
 * </ul>
 */
@Component
public class MemoryWritePolicy {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 300;

    /** Conteúdo formado só por estas palavras é cumprimento/confirmação, não informação. */
    private static final Set<String> TRIVIAL_WORDS = Set.of("oi", "ola", "bom", "boa", "dia", "tarde", "noite",
            "ok", "okay", "obrigado", "obrigada", "valeu", "sim", "nao", "tchau", "tudo", "bem", "e", "ai", "certo");
    private static final Pattern SCHEDULED = Pattern.compile(
            "(agendad|marcad|confirmad|cancelad|remarcad)[a-z]*.*(\\d{1,2}[:h]\\d{0,2}|\\d{1,2}/\\d{1,2}"
                    + "|segunda|terca|quarta|quinta|sexta|sabado|domingo|amanha|hoje)"
                    + "|(\\d{1,2}[:h]\\d{0,2}|\\d{1,2}/\\d{1,2}).*(agendad|marcad|confirmad|cancelad|remarcad)");
    private static final Pattern DOCUMENT = Pattern.compile(
            "\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b|\\b(?:\\d[ -]?){13,19}\\b");

    public Optional<String> rejectionReason(MemoryType type, String content) {
        if (type == null) {
            return Optional.of("tipo de memória inválido (use PREFERENCE, FACT, PROFILE ou CONTEXTUAL)");
        }
        if (content == null || content.isBlank()) {
            return Optional.of("conteúdo vazio");
        }
        String trimmed = content.trim();
        if (trimmed.length() < MIN_LENGTH) {
            return Optional.of("informação curta demais para ser útil");
        }
        if (trimmed.length() > Math.min(MAX_LENGTH, AgentMemory.MAX_CONTENT_LENGTH)) {
            return Optional.of("memória longa demais; resuma em uma frase");
        }
        String normalized = normalize(trimmed);
        if (isTrivial(normalized)) {
            return Optional.of("cumprimento/conteúdo sem informação duradoura");
        }
        if (SCHEDULED.matcher(normalized).find()) {
            return Optional.of("agendamentos e status ficam na agenda do CRM, não na memória");
        }
        if (DOCUMENT.matcher(trimmed).find()) {
            return Optional.of("não registre documentos pessoais (CPF, cartão) na memória");
        }
        return Optional.empty();
    }

    private static boolean isTrivial(String normalized) {
        String words = normalized.replaceAll("[^a-z ]", " ").trim();
        return words.isEmpty() || Arrays.stream(words.split("\\s+")).allMatch(TRIVIAL_WORDS::contains);
    }

    /** Normalização usada também para deduplicar memórias equivalentes. */
    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
