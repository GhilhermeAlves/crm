package com.becommerce.crm.application.ai.tool.write;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Conversoes de argumentos das write tools (AI-05). Os argumentos vem do LLM
 * como valores JSON (String/Number/Boolean). Saidas sao normalizadas e
 * validadas antes de persistir na proposta.
 */
final class ToolArgument {

    private ToolArgument() {
    }

    static UUID uuid(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return UUID.fromString(String.valueOf(value));
    }

    static LocalDateTime dateTime(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return LocalDateTime.parse(String.valueOf(value));
    }

    static String text(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.isBlank() ? null : s;
    }

    static <E extends Enum<E>> E enumValue(Map<String, Object> args, String key, Class<E> type) {
        Object value = args.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return Enum.valueOf(type, String.valueOf(value));
    }

    static BigDecimal decimal(Map<String, Object> args, String key) {
        Object value = args.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    static Map<String, Object> put(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
        return map;
    }

    static Map<String, Object> newMap() {
        return new LinkedHashMap<>();
    }
}