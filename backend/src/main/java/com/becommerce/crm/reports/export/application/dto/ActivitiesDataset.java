package com.becommerce.crm.reports.export.application.dto;

import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.ActivityRow;

import java.util.List;

/** Atividades registradas no período: contagem por tipo e lista detalhada. */
public record ActivitiesDataset(List<TypeCount> byType, List<ActivityRow> rows) {

    /** Tipo da atividade (ActivityType) com a quantidade no período. */
    public record TypeCount(String type, long count) {}

    public long count() {
        return rows.size();
    }
}
