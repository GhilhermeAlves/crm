package com.becommerce.crm.automation.campaign.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Personaliza o corpo da campanha por destinatário. Variáveis suportadas:
 * {@code {{nome}}} (nome completo), {@code {{primeiroNome}}}, {@code {{sobrenome}}},
 * {@code {{email}}} e {@code {{telefone}}}. Aceita espaços internos ({@code {{ nome }}}).
 * Variável desconhecida ou sem valor vira string vazia, para nunca enviar marcação crua.
 */
public final class CampaignMessageRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");

    private CampaignMessageRenderer() {
    }

    public record Profile(String firstName, String lastName, String email, String phone) {}

    public static String render(String body, Profile profile) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        Map<String, String> values = variables(profile);
        Matcher matcher = PLACEHOLDER.matcher(body);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    static Map<String, String> variables(Profile profile) {
        Map<String, String> values = new HashMap<>();
        if (profile == null) {
            return values;
        }
        String first = trim(profile.firstName());
        String last = trim(profile.lastName());
        String full = (first + " " + last).trim();
        values.put("nome", full);
        values.put("primeiroNome", first.isEmpty() ? "" : first.split("\\s+")[0]);
        values.put("sobrenome", last);
        values.put("email", trim(profile.email()));
        values.put("telefone", trim(profile.phone()));
        return values;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
