package com.becommerce.crm.application.ai.context;

import com.becommerce.crm.domain.ai.AiRecordType;

import java.util.UUID;

/**
 * Contexto do registro em foco (AI-02): o tipo e o id do dado que o usuário
 * está visualizando. A resolução do conteúdo real é feita pelo resolver
 * correspondente, respeitando a permissão de leitura.
 */
public record AiRecordContext(AiRecordType type, UUID recordId) {
}