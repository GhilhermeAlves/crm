package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.storage.application.dto.StorageDownload;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.reports.export.application.dto.ReportCatalogEntry;
import com.becommerce.crm.reports.export.application.dto.ReportExportRequest;
import com.becommerce.crm.reports.export.application.dto.ReportExportResponse;
import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.application.service.render.ReportFieldMasker;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.reports.export.domain.ReportExportType;
import com.becommerce.crm.reports.export.domain.ReportPeriodMode;
import com.becommerce.crm.reports.export.domain.exception.ReportExportConflictException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportNotFoundException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportRateLimitException;
import com.becommerce.crm.reports.export.domain.exception.ReportExportValidationException;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
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

    static final int MAX_MONTHS = ReportExportType.Defaults.MAX_MONTHS;
    private static final int RECENT_LIMIT = 20;

    static final int DEFAULT_MAX_PENDING = 3;
    static final int DEFAULT_MAX_PER_HOUR = 30;

    private final ReportExportRepository repository;
    private final StorageUseCase storage;
    private final ApplicationEventPublisher events;
    private final int maxPending;
    private final int maxPerHour;
    private final ReportExportAuditor auditor;

    public ReportExportService(ReportExportRepository repository, StorageUseCase storage,
                               ApplicationEventPublisher events, ReportExportAuditor auditor) {
        this(repository, storage, events, auditor, DEFAULT_MAX_PENDING, DEFAULT_MAX_PER_HOUR);
    }

    @Autowired
    public ReportExportService(ReportExportRepository repository, StorageUseCase storage,
                               ApplicationEventPublisher events, ReportExportAuditor auditor,
                               @Value("${app.reports.export-max-pending-per-user:3}") int maxPending,
                               @Value("${app.reports.export-max-per-hour-per-user:30}") int maxPerHour) {
        this.repository = repository;
        this.storage = storage;
        this.events = events;
        this.auditor = auditor;
        this.maxPending = maxPending;
        this.maxPerHour = maxPerHour;
    }

    /** Relatórios do catálogo que o usuário pode gerar (CRM_ADMIN vê todos). */
    public List<ReportCatalogEntry> catalog(Collection<String> roles, Collection<String> permissions) {
        return Arrays.stream(ReportExportType.values())
                .filter(type -> type.allowedFor(roles, permissions))
                .map(ReportCatalogEntry::from)
                .toList();
    }

    @Transactional
    public ReportExportResponse request(UUID companyId, UUID userId, Collection<String> permissions,
                                        ReportExportRequest request) {
        return request(companyId, userId, List.of(), permissions, request);
    }

    /** Valida tipo, permissão, formato e período a partir do catálogo ({@link ReportExportType}). */
    @Transactional
    public ReportExportResponse request(UUID companyId, UUID userId, Collection<String> roles,
                                        Collection<String> permissions, ReportExportRequest request) {
        ReportExportType type = parseType(request.type());
        ExportFormat format = parseFormat(request.format());
        requirePermission(type, roles, permissions);
        if (!type.supports(format)) {
            throw new ReportExportValidationException("O relatório " + type.label()
                    + " não pode ser gerado em " + format.label() + ".");
        }
        LocalDate from;
        LocalDate to;
        if (type.periodMode() == ReportPeriodMode.MONTH) {
            YearMonth month = parseMonth(request.month() != null ? request.month() : monthOf(request.from()));
            from = month.atDay(1);
            to = month.atEndOfMonth();
        } else {
            from = parseDate(request.from(), "inicial");
            to = parseDate(request.to(), "final");
            if (to.isBefore(from)) {
                throw new ReportExportValidationException("A data final deve ser igual ou posterior à inicial.");
            }
            int maxMonths = type.maxMonths();
            if (!to.isBefore(from.plusMonths(maxMonths))) {
                throw new ReportExportValidationException("O período máximo é de " + maxMonths + " meses.");
            }
        }
        // Decide agora (o gerador assíncrono não tem o usuário) se o arquivo sai sem máscara LGPD.
        boolean unmasked = ReportFieldMasker.canSeeUnmasked(roles, permissions);
        ReportExport export = ReportExport.pending(companyId, userId, type, format, from, to, unmasked);
        try {
            TenantContext.setCompanyId(companyId);
            requireWithinLimits(companyId, userId);
            repository.insert(export);
        } finally {
            TenantContext.clear();
        }
        auditor.record(ReportExportAuditor.Event.REQUESTED, export);
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

    /**
     * Cancela uma exportação PENDING do próprio usuário. Inexistente ou de
     * outro usuário é 404; já finalizada é 409. A gravação é condicional para
     * não atropelar o gerador que termine no mesmo instante.
     */
    @Transactional
    public ReportExportResponse cancel(UUID companyId, UUID userId, UUID exportId) {
        ReportExport export = owned(companyId, userId, exportId);
        if (export.status() != ReportExportStatus.PENDING) {
            throw notCancelable();
        }
        export.markCanceled();
        boolean updated;
        try {
            TenantContext.setCompanyId(companyId);
            updated = repository.updateIfPending(export);
        } finally {
            TenantContext.clear();
        }
        if (!updated) {
            throw notCancelable();
        }
        auditor.record(ReportExportAuditor.Event.CANCELED, export);
        return ReportExportResponse.from(export);
    }

    private static ReportExportConflictException notCancelable() {
        return new ReportExportConflictException("Só é possível cancelar exportações que ainda estão sendo geradas.");
    }

    /** Limites por usuário e empresa, contados no banco (vale entre instâncias). */
    private void requireWithinLimits(UUID companyId, UUID userId) {
        if (repository.countPending(companyId, userId) >= maxPending) {
            throw new ReportExportRateLimitException("Você já tem " + maxPending
                    + " relatórios sendo gerados. Aguarde a conclusão ou cancele algum antes de pedir outro.");
        }
        Instant since = Instant.now().minus(Duration.ofHours(1));
        if (repository.countRequestedSince(companyId, userId, since) >= maxPerHour) {
            throw new ReportExportRateLimitException("Limite de " + maxPerHour
                    + " relatórios por hora atingido. Tente novamente mais tarde.");
        }
    }

    /** Arquivo pronto do próprio usuário; qualquer outro caso é 404. */
    public StorageDownload download(UUID companyId, UUID userId, Collection<String> permissions, UUID exportId) {
        return download(companyId, userId, List.of(), permissions, exportId);
    }

    public StorageDownload download(UUID companyId, UUID userId, Collection<String> roles,
                                    Collection<String> permissions, UUID exportId) {
        ReportExport export = owned(companyId, userId, exportId);
        if (export.status() != ReportExportStatus.READY || export.storageObjectId() == null) {
            throw new ReportExportNotFoundException(exportId);
        }
        try {
            requirePermission(export.type(), roles, permissions);
        } catch (CrmAccessDeniedException ex) {
            auditor.record(ReportExportAuditor.Event.ACCESS_DENIED, export, userId);
            throw ex;
        }
        StorageDownload file = storage.download(companyId, export.storageObjectId());
        auditor.record(ReportExportAuditor.Event.DOWNLOADED, export, userId);
        return new StorageDownload(file.id(), export.fileName(), export.format().contentType(), file.sizeBytes(),
                file.data());
    }

    /**
     * Exportação do próprio usuário. De outro usuário da empresa é 404 para
     * quem pediu, mas fica auditada como ACCESS_DENIED.
     */
    private ReportExport owned(UUID companyId, UUID userId, UUID exportId) {
        ReportExport export;
        try {
            TenantContext.setCompanyId(companyId);
            export = repository.findById(companyId, exportId)
                    .orElseThrow(() -> new ReportExportNotFoundException(exportId));
        } finally {
            TenantContext.clear();
        }
        if (!export.requestedBy().equals(userId)) {
            auditor.record(ReportExportAuditor.Event.ACCESS_DENIED, export, userId);
            throw new ReportExportNotFoundException(exportId);
        }
        return export;
    }

    /** CRM_ADMIN sempre passa (regra do projeto); os demais precisam da permissão do tipo. */
    private static void requirePermission(ReportExportType type, Collection<String> roles,
                                          Collection<String> permissions) {
        if (!type.allowedFor(roles, permissions)) {
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
            throw new ReportExportValidationException("O formato é obrigatório (PDF, XLSX ou CSV).");
        }
        try {
            return ExportFormat.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ReportExportValidationException("Formato inválido: " + raw + ". Use PDF, XLSX ou CSV.");
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
