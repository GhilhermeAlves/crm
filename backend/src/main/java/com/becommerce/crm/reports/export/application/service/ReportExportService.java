package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.storage.application.dto.StorageDownload;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.reports.export.application.dto.ReportExportRequest;
import com.becommerce.crm.reports.export.application.dto.ReportExportResponse;
import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import com.becommerce.crm.reports.export.domain.exception.ReportExportNotFoundException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Pedidos de impressão/exportação de relatórios. O pedido é gravado PENDING e
 * a geração roda em segundo plano ({@link ReportExportGenerator}) após o
 * commit; o arquivo fica no storage e o download só é liberado ao próprio
 * solicitante, com a mesma permissão exigida pelo tipo do relatório.
 */
@Service
public class ReportExportService {

    static final int MAX_MONTHS = 12;
    private static final int RECENT_LIMIT = 20;

    private final ReportExportRepository repository;
    private final StorageUseCase storage;
    private final ApplicationEventPublisher events;

    public ReportExportService(ReportExportRepository repository, StorageUseCase storage,
                               ApplicationEventPublisher events) {
        this.repository = repository;
        this.storage = storage;
        this.events = events;
    }

    @Transactional
    public ReportExportResponse request(UUID companyId, UUID userId, Collection<String> permissions,
                                        ReportExportRequest request) {
        ReportExportType type = parseType(request.type());
        ExportFormat format = parseFormat(request.format());
        requirePermission(type, permissions);
        LocalDate from;
        LocalDate to;
        if (type == ReportExportType.MONTHLY_DRE) {
            YearMonth month = parseMonth(request.month() != null ? request.month() : monthOf(request.from()));
            from = month.atDay(1);
            to = month.atEndOfMonth();
        } else {
            from = parseDate(request.from(), "inicial");
            to = parseDate(request.to(), "final");
            if (to.isBefore(from)) {
                throw new ReportExportValidationException("A data final deve ser igual ou posterior à inicial.");
            }
            if (!to.isBefore(from.plusMonths(MAX_MONTHS))) {
                throw new ReportExportValidationException("O período máximo é de " + MAX_MONTHS + " meses.");
            }
        }
        ReportExport export = ReportExport.pending(companyId, userId, type, format, from, to);
        try {
            TenantContext.setCompanyId(companyId);
            repository.insert(export);
        } finally {
            TenantContext.clear();
        }
        events.publishEvent(new ReportExportRequestedEvent(companyId, export.id()));
        return ReportExportResponse.from(export);
    }

    @Transactional(readOnly = true)
    public List<ReportExportResponse> recent(UUID companyId, UUID userId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findRecent(companyId, userId, RECENT_LIMIT).stream()
                    .map(ReportExportResponse::from)
                    .toList();
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public ReportExportResponse get(UUID companyId, UUID userId, UUID exportId) {
        return ReportExportResponse.from(owned(companyId, userId, exportId));
    }

    /** Arquivo pronto do próprio usuário; qualquer outro caso é 404. */
    public StorageDownload download(UUID companyId, UUID userId, Collection<String> permissions, UUID exportId) {
        ReportExport export = owned(companyId, userId, exportId);
        if (export.status() != ReportExportStatus.READY || export.storageObjectId() == null) {
            throw new ReportExportNotFoundException(exportId);
        }
        requirePermission(export.type(), permissions);
        StorageDownload file = storage.download(companyId, export.storageObjectId());
        return new StorageDownload(file.id(), export.fileName(), export.format().contentType(), file.sizeBytes(),
                file.data());
    }

    private ReportExport owned(UUID companyId, UUID userId, UUID exportId) {
        try {
            TenantContext.setCompanyId(companyId);
            return repository.findById(companyId, exportId)
                    .filter(export -> export.requestedBy().equals(userId))
                    .orElseThrow(() -> new ReportExportNotFoundException(exportId));
        } finally {
            TenantContext.clear();
        }
    }

    private static void requirePermission(ReportExportType type, Collection<String> permissions) {
        if (permissions == null || !permissions.contains(type.requiredPermission())) {
            throw new CrmAccessDeniedException("Você não tem permissão para gerar o relatório "
                    + type.label() + ".");
        }
    }

    private static ReportExportType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ReportExportValidationException("O tipo do relatório é obrigatório.");
        }
        try {
            return ReportExportType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ReportExportValidationException("Tipo de relatório inválido: " + raw + ".");
        }
    }

    private static ExportFormat parseFormat(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ReportExportValidationException("O formato é obrigatório (PDF ou XLSX).");
        }
        try {
            return ExportFormat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ReportExportValidationException("Formato inválido: " + raw + ". Use PDF ou XLSX.");
        }
    }

    private static String monthOf(String date) {
        return date == null || date.length() < 7 ? date : date.substring(0, 7);
    }

    private static YearMonth parseMonth(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ReportExportValidationException("O mês do relatório é obrigatório.");
        }
        try {
            return YearMonth.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new ReportExportValidationException("Mês inválido: " + raw + ". Use o formato aaaa-mm.");
        }
    }

    private static LocalDate parseDate(String raw, String which) {
        if (raw == null || raw.isBlank()) {
            throw new ReportExportValidationException("A data " + which + " é obrigatória.");
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new ReportExportValidationException("Data " + which + " inválida: " + raw + ".");
        }
    }
}
