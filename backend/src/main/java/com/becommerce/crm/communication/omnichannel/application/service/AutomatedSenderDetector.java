package com.becommerce.crm.communication.omnichannel.application.service;

import com.becommerce.crm.communication.omnichannel.domain.Message;
import com.becommerce.crm.communication.omnichannel.domain.MessageDirection;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reconhece quando quem escreve é outro robô (resposta automática, menu de
 * atendimento, assistente virtual) para o agente não ficar conversando com ele.
 * Heurística conservadora: na dúvida, trata como pessoa — o próprio agente
 * ainda pode sinalizar o robô pela ferramenta {@code marcar_como_robo}.
 */
public final class AutomatedSenderDetector {

    private static final List<String> AUTOMATED_PHRASES = List.of(
            "mensagem automatica",
            "resposta automatica",
            "atendimento automatico",
            "atendimento automatizado",
            "assistente virtual",
            "sou um robo",
            "sou um bot",
            "sou o bot",
            "chatbot",
            "digite o numero",
            "digite a opcao",
            "digite uma das opcoes",
            "digite apenas o numero",
            "escolha uma das opcoes",
            "selecione uma opcao",
            "selecione uma das opcoes",
            "responda com o numero",
            "responda apenas com o numero",
            "nao responda esta mensagem",
            "this is an automated");

    /** Linha de menu: "1 - Agendar", "2) Financeiro", "a. Suporte". */
    private static final Pattern MENU_LINE = Pattern.compile("^\\s*(\\d{1,2}|[a-e])\\s*[-.):–]\\s*\\S.*$");

    /** Emoji de número: 1️⃣ = "1" + U+FE0F + U+20E3. */
    private static final char KEYCAP = '⃣';

    /** Mensagem idêntica repetida com resposta nossa no meio: ninguém digita igual duas vezes. */
    private static final int MIN_REPEATED_LENGTH = 15;

    private AutomatedSenderDetector() {
    }

    /**
     * @param current texto da mensagem que acabou de chegar
     * @param recent  histórico recente da conversa em ordem cronológica (pode
     *                incluir a própria mensagem atual)
     * @return o motivo, quando a mensagem parece vir de um robô
     */
    public static Optional<String> detect(String current, List<Message> recent) {
        if (current == null || current.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(current);
        for (String phrase : AUTOMATED_PHRASES) {
            if (normalized.contains(phrase)) {
                return Optional.of("frase de mensagem automática (\"" + phrase + "\")");
            }
        }
        long menuLines = normalized.lines().filter(line -> MENU_LINE.matcher(line).matches()).count();
        // Emojis de número (1️⃣ 2️⃣) perdem o "⃣" na normalização: conta no texto original.
        long keycaps = current.chars().filter(c -> c == KEYCAP).count();
        if (menuLines >= 2 || keycaps >= 2) {
            return Optional.of("menu numerado de atendimento automático");
        }
        if (normalized.length() >= MIN_REPEATED_LENGTH && repeatedAfterOurReply(normalized, recent)) {
            return Optional.of("mesma mensagem repetida após a nossa resposta");
        }
        return Optional.empty();
    }

    /** A mesma mensagem já tinha chegado antes e nós respondemos entre as duas. */
    private static boolean repeatedAfterOurReply(String normalized, List<Message> recent) {
        if (recent == null || recent.isEmpty()) {
            return false;
        }
        boolean outboundSince = false;
        for (int i = recent.size() - 1; i >= 0; i--) {
            Message m = recent.get(i);
            if (m.getDirection() == MessageDirection.OUTBOUND) {
                outboundSince = true;
                continue;
            }
            if (outboundSince && m.getBody() != null && normalize(m.getBody()).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    static String normalize(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return decomposed.toLowerCase(Locale.ROOT).replaceAll("[ \\t]+", " ").trim();
    }
}
