package com.becommerce.crm.domain.workflow;

/**
 * Status de uma execução de workflow (Item 7 / Item 6). PROCESSING é usado
 * apenas transitoriamente dentro da transação de execução (nunca persistido em
 * repouso); SKIPPED pode ser registrado quando a idempotência impede re-execução.
 */
public enum ExecutionStatus {
    PROCESSING,
    SUCCESS,
    FAILED,
    SKIPPED
}
