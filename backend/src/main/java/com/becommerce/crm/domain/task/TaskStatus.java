package com.becommerce.crm.domain.task;

/**
 * Status de uma Task (Sprint 12). Fluxo: PENDING → IN_PROGRESS → COMPLETED;
 * CANCELLED encerra sem concluir. Reutiliza a nomenclatura do projeto.
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}