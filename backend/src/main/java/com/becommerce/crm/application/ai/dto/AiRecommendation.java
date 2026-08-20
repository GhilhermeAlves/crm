package com.becommerce.crm.application.ai.dto;

/**
 * Recomendação / próxima melhor ação (AI-06). A IA NUNCA executa a ação: ela é
 * apenas sugerida. Se futuramente for executável, deve passar pelo mecanismo de
 * proposta/confirmação da AI-05 ({@code PROPOSED → CONFIRMED/CANCELLED}).
 *
 * @param key           chave canônica da recomendação
 * @param title         título curto (ex.: "Fazer follow-up")
 * @param description   descrição detalhada
 * @param priority      prioridade (opcional, ex.: 0..100)
 * @param justification justificativa baseada em fatos/inferências
 * @param action        ação correspondente (se existir), nunca executada
 */
public record AiRecommendation(String key, String title, String description, Integer priority,
                               String justification, String action) {
}