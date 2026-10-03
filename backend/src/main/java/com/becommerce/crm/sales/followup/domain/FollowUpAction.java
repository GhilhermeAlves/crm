package com.becommerce.crm.sales.followup.domain;

/**
 * Tipo de ação executada por um FollowUp. Abstração preparada para futuras
 * Sequences: novas ações entram aqui (e no processador) sem reescrever o
 * domínio/scheduler.
 */
public enum FollowUpAction {
    /** Envia uma mensagem de texto via provider do canal da conversa. */
    SEND_MESSAGE
}