package com.becommerce.crm.reports.export.application.dto;

import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.ContactRow;

import java.util.List;

/** Contatos cadastrados no período, em ordem de cadastro. */
public record ContactsDataset(List<ContactRow> rows) {

    public long count() {
        return rows.size();
    }
}
