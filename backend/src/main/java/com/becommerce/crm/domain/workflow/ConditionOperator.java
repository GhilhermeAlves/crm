package com.becommerce.crm.domain.workflow;

/**
 * Operadores de condição suportados (Item 3). Primeira versão contém apenas os
 * operadores necessários; não há linguagem de expressão completa.
 */
public enum ConditionOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_OR_EQUAL,
    LESS_OR_EQUAL,
    /** Contém (case-insensitive) — Sprint 18. */
    CONTAINS,
    /** Campo ausente/nulo no contexto — Sprint 18. */
    IS_NULL,
    /** Campo presente no contexto — Sprint 18. */
    IS_NOT_NULL
}
