package com.becommerce.crm.domain.campaign;

/** Status da execução de uma campanha (tabela {@code campaign_executions}, V058). */
public enum ExecutionStatus {
    RUNNING,
    PAUSED,
    COMPLETED,
    CANCELLED
}
