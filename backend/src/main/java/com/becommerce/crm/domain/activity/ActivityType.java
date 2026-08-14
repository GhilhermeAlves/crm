package com.becommerce.crm.domain.activity;

/**
 * Tipos de atividade comercial (Sprint 12). Uma Activity representa uma
 * interação/acontecimento relevante do relacionamento comercial. Os tipos são
 * extensíveis; ingestion futura do Inbox (email/whatsapp) criará activities com
 * esses tipos sem alterar o modelo (o Inbox apenas registra a Activity).
 */
public enum ActivityType {
    CALL,
    MEETING,
    EMAIL,
    MESSAGE,
    NOTE,
    PROPOSAL,
    FOLLOW_UP,
    OTHER
}