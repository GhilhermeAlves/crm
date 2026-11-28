package com.becommerce.crm.reports.export.application.dto;

import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.LeadRow;

import java.util.List;

/** Leads criados no período: contagem por status e lista detalhada. */
public record LeadsDataset(List<StatusCount> byStatus, List<LeadRow> rows) {

    /** Status do lead (LeadStatus) com a quantidade no período. */
    public record StatusCount(String status, long count) {}

    public long count() {
        return rows.size();
    }
}
