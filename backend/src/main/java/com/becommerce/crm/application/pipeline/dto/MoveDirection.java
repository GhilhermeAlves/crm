package com.becommerce.crm.application.pipeline.dto;

import jakarta.validation.constraints.NotNull;

/** Direção de movimentação de uma oportunidade entre estágios (P-020). */
public enum MoveDirection {
    ADVANCE,
    REGRESS
}
