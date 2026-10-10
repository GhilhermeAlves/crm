package com.becommerce.crm.finance.report.application.dto;

import java.util.List;

/**
 * Relatório de agenda do período. {@code statuses} aceita CANCELADO, CONFIRMADO e
 * PENDENTE (mapeados para os status reais da agenda: CANCELED, CONFIRMED e
 * SCHEDULED). attended = COMPLETED, notAttended = NO_SHOW;
 * attendanceRate = attended / (attended + notAttended). {@code total} e
 * {@code byStatus} refletem todos os status da agenda encontrados no período.
 */
public record AgendaReportResponse(
        long total,
        long attended,
        long notAttended,
        double attendanceRate,
        List<StatusCountItem> byStatus) {

    public record StatusCountItem(String status, long count) {}
}