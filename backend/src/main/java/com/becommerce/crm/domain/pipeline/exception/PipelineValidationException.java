package com.becommerce.crm.domain.pipeline.exception;

/**
 * Violação de regra de negócio do pipeline (P-002/P-003/P-020/P-021/P-022).
 * Resulta em HTTP 400 — erro de validação de domínio, não de infraestrutura.
 */
public class PipelineValidationException extends RuntimeException {

    public PipelineValidationException(String message) {
        super(message);
    }
}
