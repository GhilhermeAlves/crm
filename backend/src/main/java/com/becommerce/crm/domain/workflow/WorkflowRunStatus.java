package com.becommerce.crm.domain.workflow;

/**
 * Status de uma execução em nível de RULE ({@code workflow_runs}, Sprint 15).
 *
 * <p>Complementa o {@link ExecutionStatus} (por ação): representa o desfecho da
 * avaliação da regra para um evento. MATCHED é transitório (condições ok, ações
 * em execução) e é substituído logo após; nunca é intencionalmente persistido em
 * repouso.
 */
public enum WorkflowRunStatus {
    /** Condições atendidas; ações em execução (transitório). */
    MATCHED,
    /** Todas as ações concluíram com sucesso. */
    SUCCESS,
    /** Parte das ações falhou (pelo menos uma, mas não todas). */
    PARTIAL,
    /** Todas as ações falharam (ou erro em nível de regra). */
    FAILED,
    /** Regra ignorada por não atender a alguma condição. */
    SKIPPED
}
