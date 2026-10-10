package com.becommerce.crm.finance.accounting.application.dto;

/**
 * Corpo da requisição de geração de relatório contábil (Etapa 3). Campos como
 * {@code String} para validar manualmente e responder 400 via
 * {@code FinancialTransactionValidationException} em qualquer valor inválido.
 */
public record AccountingReportRequestDto(String type, String month, String format) {
}