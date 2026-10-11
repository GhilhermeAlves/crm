package com.becommerce.crm.finance.transaction.domain.exception;

/** Payload ou transição de estado inválidos de um lançamento financeiro (resposta 400). */
public class FinancialTransactionValidationException extends RuntimeException {

    public FinancialTransactionValidationException(String message) {
        super(message);
    }
}
