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

    /**
     * Grava o estado final só se o registro ainda estiver PENDING no banco.
     * Retorna {@code false} quando outro fluxo (cancelamento, job de travados)
     * já finalizou a exportação.
     */
    boolean updateIfPending(ReportExport export);

    Optional<ReportExport> findById(UUID companyId, UUID id);

    List<ReportExport> findRecent(UUID companyId, UUID requestedBy, int limit);

    /** Exportações READY da empresa finalizadas antes de {@code before} (candidatas ao expurgo). */
    List<ReportExport> findReadyFinishedBefore(UUID companyId, Instant before, int limit);

    /** Exportações PENDING da empresa criadas antes de {@code before} (geração travada). */
    List<ReportExport> findPendingCreatedBefore(UUID companyId, Instant before, int limit);

    /** Quantas exportações PENDING o usuário tem na empresa. */
    int countPending(UUID companyId, UUID requestedBy);

    /** Quantos pedidos o usuário fez na empresa desde {@code since}. */
    int countRequestedSince(UUID companyId, UUID requestedBy, Instant since);
}
