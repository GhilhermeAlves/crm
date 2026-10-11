package com.becommerce.crm.finance.report.application.port.output;

import java.util.List;
import java.util.UUID;

/** Persistência de {@code report_suggestions} (V094). RLS FORCE isola o tenant. */
public interface ReportSuggestionRepository {

    UUID create(UUID companyId, List<String> categories, String notes, UUID createdBy);
}
