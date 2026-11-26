package com.becommerce.crm.automation.ai.application.port.output;

/**
 * Mídia para o agente: entender o que o paciente mandou (áudio, imagem, PDF)
 * e gerar voz para responder. Falhas lançam
 * {@link com.becommerce.crm.automation.ai.domain.AiProviderException}.
 */
public interface AiMediaProvider {

    /** Transcreve um áudio (ex.: nota de voz do WhatsApp, ogg/opus) para texto em português. */
    String transcribe(byte[] audio, String mimeType);

    /**
     * Lê uma imagem ou PDF e devolve um resumo textual do conteúdo, seguindo
     * {@code instruction} (ex.: "descreva sem dar diagnóstico").
     */
    String describe(byte[] content, String mimeType, String fileName, String instruction);

    /** Resposta estruturada de visão: JSON bruto + modelo e tokens para auditoria. */
    record VisionJsonResult(String json, String model, Integer inputTokens, Integer outputTokens) {
    }

    /**
     * Analisa uma imagem exigindo resposta em JSON (modo JSON do provedor quando
     * houver). {@code model} nulo usa o modelo de visão padrão. O default
     * delega a {@link #describe} para provedores sem suporte.
     */
    default VisionJsonResult analyzeImageJson(byte[] image, String mimeType, String systemPrompt, String userPrompt,
                                              String model, int maxTokens) {
        return new VisionJsonResult(describe(image, mimeType, null, systemPrompt + "\n\n" + userPrompt),
                null, null, null);
    }

    /** Gera a fala de {@code text} em áudio ogg/opus (formato de nota de voz do WhatsApp). */
    byte[] speech(String text);
}
