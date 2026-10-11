package com.becommerce.crm.finance.accounting.application.dto;

/**
 * Resultado da importação de lançamentos via CSV (Etapa 3). Linhas válidas são
 * gravadas mesmo quando outras falham; cada falha vira um item de {@code errors}.
 */
public record CsvImportResult(int imported, java.util.List<CsvLineError> errors) {
}