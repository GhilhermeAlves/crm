package com.becommerce.crm.finance.transaction.application.service;

import java.util.UUID;

/** Publicado após baixa, cancelamento ou exclusão de um lançamento (ouvido após o commit). */
public record FinancialTransactionChangedEvent(UUID companyId, UUID transactionId, Kind kind) {
    public enum Kind { PAID, CANCELLED, DELETED }
}
