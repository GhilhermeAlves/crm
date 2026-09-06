package com.becommerce.crm.domain.followup;

/**
 * Motivo do cancelamento de um FollowUp. {@code USER} é o cancelamento manual
 * pelo atendente; {@code HUMAN_MODE} e {@code SUPERSEDED_BY_NEW_MESSAGE} são
 * cancelamentos por regra de segurança do processador (nunca ignorar Human
 * Takeover nem enviar follow-up obsoleto após resposta do cliente).
 */
public enum FollowUpCancellationReason {
    USER,
    HUMAN_MODE,
    SUPERSEDED_BY_NEW_MESSAGE
}