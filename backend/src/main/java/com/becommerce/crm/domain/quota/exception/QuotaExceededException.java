package com.becommerce.crm.domain.quota.exception;

/**
 * Lançada quando uma ação SaaS viola o limite do plano da empresa
 * (Sprint 8.6): max_users, max_contacts ou max_storage_mb.
 *
 * <p>Mapeada para HTTP 422 Unprocessable Entity no GlobalExceptionHandler, com
 * mensagem clara exibida ao frontend.
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}