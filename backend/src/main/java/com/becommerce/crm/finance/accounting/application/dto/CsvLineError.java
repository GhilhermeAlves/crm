package com.becommerce.crm.finance.accounting.application.dto;

/**
 * Erro de uma linha do CSV de importação (Etapa 3). {@code line} é a linha
 * física do arquivo (1-based, cabeçalho = linha 1).
 */
public record CsvLineError(int line, String message) {
}