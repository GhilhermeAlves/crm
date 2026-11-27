package com.becommerce.crm.reports.export.application.port.out;

import com.becommerce.crm.reports.export.domain.ReportExport;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistência de {@code report_exports} (V096), isolada por RLS. */
public interface ReportExportRepository {

    void insert(ReportExport export);

    void update(ReportExport export);

    Optional<ReportExport> findById(UUID companyId, UUID id);

    List<ReportExport> findRecent(UUID companyId, UUID requestedBy, int limit);

    /** Exportações READY da empresa finalizadas antes de {@code before} (candidatas ao expurgo). */
    List<ReportExport> findReadyFinishedBefore(UUID companyId, Instant before, int limit);
}
