package com.becommerce.crm.finance.report.application.dto;

import java.util.List;

/**
 * Corpo de {@code POST /report-suggestions}. Campos em {@code String} para
 * validar manualmente e responder 400 (evita HttpMessageNotReadableException,
 * que hoje viraria 500).
 */
public record CreateReportSuggestionRequest(List<String> categories, String notes) {
}
