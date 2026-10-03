package com.becommerce.crm.automation.campaign.domain;

/** Status da execução de uma campanha (tabela {@code campaign_executions}, V058). */
public enum ExecutionStatus {
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}
