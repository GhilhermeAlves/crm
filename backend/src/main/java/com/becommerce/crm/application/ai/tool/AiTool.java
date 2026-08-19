package com.becommerce.crm.application.ai.tool;

import com.becommerce.crm.application.ai.port.output.AiProvider;

import java.util.Map;

/**
 * Abstração de uma Tool de IA (AI-03): camada de integração entre o modelo e o
 * CRM. Cada Tool reutiliza services/repositories existentes, respeita
 * tenant/CurrentUser/permissions e retorna dados estruturados.
 *
 * <p>As Tools pertencem ao domínio da aplicação — não ao provider específico.
 * A execução é sempre intermediada pelo {@code AiToolRegistry}.</p>
 */
public interface AiTool {

    /** Nome único da Tool (usado pelo modelo no Tool Calling). */
    String name();

    /** Descrição para o modelo entender quando usar a Tool. */
    String description();

    /** Permissão de leitura exigida (nome REAL do projeto). */
    String requiredPermission();

    /** Nível de risco: {@code READ} para as read tools desta milestone. */
    String risk();

    /** Schema de entrada (estilo JSON Schema) para validar/descrever argumentos. */
    Map<String, Object> inputSchema();

    /** Definção enviada ao modelo (nome, descrição, schema). */
    AiProvider.ToolDefinition toolDefinition();

    /**
     * Executa a Tool com os argumentos (vindos do LLM) sob o contexto
     * confiável. Retorna resultado estruturado; lança em erro inesperado (o
     * registry converte em falha).
     */
    AiToolResult execute(AiToolContext ctx, Map<String, Object> arguments);
}