package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.application.ai.port.output.AiProvider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base para Read Tools (AI-03): centraliza a construção do schema de entrada
 * (JSON Schema), a definição enviada ao modelo e a conversão de argumentos
 * (UUID, string, int). Ferramentas de leitura usam {@code READ} como risco.
 */
public abstract class AbstractAiReadTool implements AiTool {

    private final String name;
    private final String description;
    private final String requiredPermission;
    private final Map<String, Object> properties;

    protected AbstractAiReadTool(String name, String description, String requiredPermission,
                                 Map<String, Object> properties) {
        this.name = name;
        this.description = description;
        this.requiredPermission = requiredPermission;
        this.properties = Map.copyOf(properties);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String requiredPermission() {
        return requiredPermission;
    }

    @Override
    public String risk() {
        return "READ";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        return schema;
    }

    @Override
    public AiProvider.ToolDefinition toolDefinition() {
        return new AiProvider.ToolDefinition(name, description, inputSchema());
    }

    /**
     * Template method de execução: converte entradas inválidas e erros
     * inesperados em {@link AiToolResult} de falha — a Tool nunca lança para o
     * modelo como se fosse dado de CRM.
     */
    @Override
    public final AiToolResult execute(AiToolContext ctx, Map<String, Object> arguments) {
        try {
            return doExecute(ctx, arguments);
        } catch (IllegalArgumentException e) {
            return AiToolResult.fail(name, e.getMessage());
        } catch (RuntimeException e) {
            return AiToolResult.fail(name, e.getMessage());
        }
    }

    /** Lógica específica da Tool; valida entradas e reutiliza o CRM. */
    protected abstract AiToolResult doExecute(AiToolContext ctx, Map<String, Object> arguments);

    // ---------------------------------------------------------------
    // Conversores de argumentos (vindos do LLM) com validação
    // ---------------------------------------------------------------

    protected String string(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v == null) {
            return null;
        }
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    protected UUID uuid(Map<String, Object> args, String key) {
        String s = string(args, key);
        if (s == null) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID inválido para '" + key + "': " + s);
        }
    }

    protected Integer integer(Map<String, Object> args, String key) {
        Object v = args == null ? null : args.get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor inválido para '" + key + "': " + v);
        }
    }

    protected static Map<String, Object> stringProp(String description, boolean required) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "string");
        p.put("description", description);
        if (required) {
            p.put("required", true);
        }
        return p;
    }

    protected static Map<String, Object> integerProp(String description) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("type", "integer");
        p.put("description", description);
        return p;
    }

    /** Limite máximo padrão para buscas (evita unbounded search). */
    protected static int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 20;
        }
        return Math.min(limit, 50);
    }
}