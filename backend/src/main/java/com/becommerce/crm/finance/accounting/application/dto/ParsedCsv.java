package com.becommerce.crm.finance.accounting.application.dto;

import java.util.List;

/**
 * Saída do parser do CSV de importação (Etapa 3): linhas válidas e erros de
 * formato por linha (uma linha inválida não derruba as demais).
 * {@code dataLineCount} é o total de linhas de dados (para o limite de 2000).
 */
public record ParsedCsv(List<CsvImportRow> rows, List<CsvLineError> errors, int dataLineCount) {
}