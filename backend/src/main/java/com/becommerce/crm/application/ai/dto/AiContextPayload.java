package com.becommerce.crm.application.ai.dto;

import java.util.UUID;

/**
 * Contexto do assistente (AI-01). Enviado pelo frontend como dica de "onde o
 * usuário está" (tela + registro em foco). O backend RESOLVE o contexto real e
 * NÃO confia cegamente — este payload apenas direciona qual dado carregar.
 */
public record AiContextPayload(
        String screen,
        UUID recordId
) {
}
