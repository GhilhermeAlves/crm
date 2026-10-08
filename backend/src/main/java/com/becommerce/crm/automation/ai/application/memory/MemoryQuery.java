package com.becommerce.crm.automation.ai.application.memory;

import java.util.UUID;

/**
 * Pedido de recuperação de memórias. O escopo (empresa + agente + contato) é
 * obrigatório; {@code currentMessage} já viaja na consulta para que uma futura
 * implementação semântica (embeddings/vector search) substitua a atual sem
 * mudar quem chama.
 */
public record MemoryQuery(UUID companyId, UUID agentConfigId, UUID contactId, String currentMessage, int limit) {

    public static final int DEFAULT_LIMIT = 5;

    public MemoryQuery {
        if (companyId == null || contactId == null) {
            throw new IllegalArgumentException("companyId e contactId são obrigatórios para recuperar memórias");
        }
        limit = limit <= 0 ? DEFAULT_LIMIT : limit;
    }
}
