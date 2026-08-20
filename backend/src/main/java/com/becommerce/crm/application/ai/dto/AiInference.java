package com.becommerce.crm.application.ai.dto;

/**
 * Inferência da AI-06 (AI-06): conclusão produzida pelo modelo a partir dos
 * fatos. Estruturalmente separada dos {@link AiFact} para que uma inferência
 * jamais seja apresentada como dado armazenado no CRM.
 *
 * @param key        chave canônica da inferência
 * @param text       texto da inferência
 * @param confidence grau de confiança (opcional, ex.: 0..100)
 */
public record AiInference(String key, String text, Integer confidence) {
}