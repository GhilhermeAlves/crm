package com.becommerce.crm.reports.export.application.dto;

/**
 * Corpo do {@code POST /report-exports}. Campos em {@code String} para validar
 * manualmente e responder 400 com mensagem pt-BR. DRE usa {@code month}
 * (yyyy-MM); os demais usam {@code from}/{@code to} (yyyy-MM-dd).
 */
public record ReportExportRequest(String type, String format, String from, String to, String month) {
}
