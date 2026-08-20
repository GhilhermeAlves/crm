package com.becommerce.crm.application.ai.dto;

/**
 * Fato derivado de dado REAL do CRM (AI-06). Nunca é criado pelo modelo: é
 * montado pelo backend a partir de repositories/services e a {@code source}
 * identifica de onde o dado foi extraído. A ausência de um dado não vira fato.
 *
 * @param key   chave canônica do fato (ex.: {@code opportunity.value})
 * @param label rótulo legível para apresentação
 * @param value valor real recuperado do CRM
 * @param source origem do dado (ex.: {@code opportunity_context})
 */
public record AiFact(String key, String label, String value, String source) {
}