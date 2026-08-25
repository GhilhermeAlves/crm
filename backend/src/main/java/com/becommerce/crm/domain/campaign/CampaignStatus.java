package com.becommerce.crm.domain.campaign;

/**
 * Ciclo de vida da campanha (Sprint 17).
 * Transições permitidas são controladas por {@link Campaign#transitionTo}.
 */
public enum CampaignStatus {
    DRAFT,
    SCHEDULED,
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}
