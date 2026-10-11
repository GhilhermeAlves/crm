package com.becommerce.crm.finance.transaction.domain.exception;

import java.util.UUID;

/** Lançamento financeiro inexistente ou pertencente a outro tenant (resposta 404). */
public class FinancialTransactionNotFoundException extends RuntimeException {

    public FinancialTransactionNotFoundException(UUID id) {
        super("Lançamento financeiro não encontrado: " + id);
    }
}
