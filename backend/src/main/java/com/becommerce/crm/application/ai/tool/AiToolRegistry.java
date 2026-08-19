package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.application.ai.port.output.AiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registro de Tools de IA (AI-03). Localiza uma Tool pelo nome, lista as
 * disponíveis, verifica permissão de leitura e executa — sempre intermediando
 * a execução (o LLM nunca executa a Tool diretamente).
 *
 * <p>Impede execução de Tool inexistente e de Tool sem a permissão exigida,
 * retornando um {@link AiToolResult} de falha (nunca lança para o modelo como
 * se fosse dado de CRM).</p>
 */
@Component
public class AiToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiToolRegistry.class);

    private final Map<String, AiTool> tools;

    public AiToolRegistry(List<AiTool> tools) {
        this.tools = tools.stream()
                .collect(Collectors.toMap(AiTool::name, Function.identity()));
    }

    public Optional<AiTool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public List<AiTool> list() {
        return tools.values().stream()
                .sorted(Comparator.comparing(AiTool::name))
                .toList();
    }

    /** Definições das Tools enviadas ao modelo no Tool Calling. */
    public List<AiProvider.ToolDefinition> toolDefinitions() {
        return list().stream().map(AiTool::toolDefinition).toList();
    }

    /**
     * Executa uma Tool sob o contexto confiável. Verifica existência e
     * permissão; converte erros inesperados em falha.
     */
    public AiToolResult execute(String name, AiToolContext ctx, Map<String, Object> arguments) {
        AiTool tool = tools.get(name);
        if (tool == null) {
            return AiToolResult.fail(name, "Ferramenta desconhecida: " + name);
        }
        if (!ctx.permissions().has(tool.requiredPermission())) {
            return AiToolResult.fail(name, "Sem permissão de leitura para: " + name);
        }
        try {
            return tool.execute(ctx, arguments);
        } catch (Exception e) {
            log.warn("Tool {} falhou: {}", name, e.getMessage());
            return AiToolResult.fail(name, "Falha ao executar a ferramenta: " + e.getMessage());
        }
    }
}