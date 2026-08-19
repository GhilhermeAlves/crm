package com.becommerce.crm.application.ai.dto;

import com.becommerce.crm.domain.ai.AiRecordType;

import java.util.UUID;

/**
 * Contexto do assistente (AI-02). Enviado pelo frontend como dica de "onde o
 * usuário está": tela, rota, tipo de registro em foco e seu id. O backend
 * RESOLVE o contexto real (via Context Engine) e NÃO confia cegamente — este
 * payload apenas direciona qual dado carregar e onde o usuário navega.
 */
public record AiContextPayload(
        String screen,
        String route,
        String recordType,
        UUID recordId
) {

    /**
     * Construtor de compatibilidade (sem rota/tipo explícito). Útil para telas
     * que apenas indicam o registro em foco.
     */
    public AiContextPayload(String screen, UUID recordId) {
        this(screen, null, null, recordId);
    }

    /** Tipo de registro em foco, normalizado; {@code null} se ausente/desconhecido. */
    public AiRecordType resolvedType() {
        return AiRecordType.from(recordType);
    }
}