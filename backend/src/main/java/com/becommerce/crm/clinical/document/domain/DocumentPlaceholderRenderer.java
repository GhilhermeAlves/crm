package com.becommerce.crm.clinical.document.domain;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Substitui variáveis {@code {{chave}}} (com ou sem espaços internos) pelos
 * valores informados. Chave desconhecida ou valor nulo viram string vazia, para
 * nunca deixar marcação crua no documento emitido.
 */
public final class DocumentPlaceholderRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.]+)\\s*}}");

    private DocumentPlaceholderRenderer() {
    }

    public static String render(String body, Map<String, String> values) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(body);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values == null ? null : values.get(key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
