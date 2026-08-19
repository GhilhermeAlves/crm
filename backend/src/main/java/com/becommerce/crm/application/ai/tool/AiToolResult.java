package com.becommerce.crm.application.ai.tool;

/**
 * Resultado estruturado da execução de uma Tool (AI-03). Carrega dados REAIS do
 * CRM (nunca texto gerado pelo LLM); a apresentação textual é responsabilidade
 * do modelo/orchestrator.
 *
 * @param name nome da Tool executada
 * @param success {@code true} se a execução obteve dados com sucesso
 * @param data dados estruturados de retorno (DTO do CRM) ou {@code null}
 * @param error mensagem de erro quando {@code success=false}
 */
public record AiToolResult(String name, boolean success, Object data, String error) {

    public static AiToolResult ok(String name, Object data) {
        return new AiToolResult(name, true, data, null);
    }

    public static AiToolResult fail(String name, String error) {
        return new AiToolResult(name, false, null, error);
    }
}