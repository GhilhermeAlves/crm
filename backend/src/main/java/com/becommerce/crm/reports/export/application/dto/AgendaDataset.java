package com.becommerce.crm.reports.export.application.dto;

import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.AppointmentRow;

import java.util.List;

/** Agenda do período: resumo por profissional e lista detalhada de consultas. */
public record AgendaDataset(List<ProfessionalSummary> professionals, List<AppointmentRow> rows) {

    public record ProfessionalSummary(String name, long scheduled, long confirmed, long completed, long noShow,
                                      long canceled) {
        public long total() {
            return scheduled + confirmed + completed + noShow + canceled;
        }
    }
}
