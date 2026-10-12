package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.becommerce.crm.finance.accounting.application.service.FinanceLabels;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.application.service.render.PdfReportLayout;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument;
import com.becommerce.crm.reports.export.application.service.render.ReportDocumentFactory;
import com.becommerce.crm.reports.export.application.service.render.XlsxReportWriter;
import com.becommerce.crm.reports.export.domain.ExportFormat;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Gera o arquivo de uma exportação PENDING: dataset → documento → PDF/XLSX →
 * storage → READY → notificação no sino com o link de download. Qualquer erro
 * marca FAILED e avisa o usuário; o registro nunca fica em PENDING.
 *
 * <p>Os serviços de storage e notificação limpam o {@link TenantContext} ao
 * terminar, então cada acesso ao banco daqui define o tenant de novo.
 */
@Component
public class ReportExportGenerator {

    public static final String NOTIFICATION_KIND = "REPORT_EXPORT";

    private static final Logger log = LoggerFactory.getLogger(ReportExportGenerator.class);
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");

    private final ReportExportRepository repository;
    private final ReportDataGateway dataGateway;
    private final ReportDatasetBuilder datasets;
    private final StorageUseCase storage;
    private final NotificationUseCase notifications;
    private final ObjectMapper objectMapper;
    private Clock clock = Clock.system(CLINIC_ZONE);

    public ReportExportGenerator(ReportExportRepository repository, ReportDataGateway dataGateway,
                                 ReportDatasetBuilder datasets, StorageUseCase storage,
                                 NotificationUseCase notifications, ObjectMapper objectMapper) {
        this.repository = repository;
        this.dataGateway = dataGateway;
        this.datasets = datasets;
        this.storage = storage;
        this.notifications = notifications;
        this.objectMapper = objectMapper;
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    public void generate(UUID companyId, UUID exportId) {
        Optional<ReportExport> found = inTenant(companyId, () -> repository.findById(companyId, exportId));
        if (found.isEmpty() || found.get().status() != ReportExportStatus.PENDING) {
            return;
        }
        ReportExport export = found.get();
        try {
            byte[] content = inTenant(companyId, () -> render(export));
            String fileName = fileName(export);
            StorageResponse stored = storage.upload(companyId, fileName, export.format().contentType(), content,
                    export.requestedBy());
            export.markReady(stored.id(), fileName);
            inTenant(companyId, () -> {
                repository.update(export);
                return null;
            });
            notifications.create(companyId, new CreateNotificationRequest(export.requestedBy(),
                    NotificationType.SYSTEM, "Relatório pronto para download", summary(export),
                    metadata(export)), export.requestedBy());
        } catch (RuntimeException ex) {
            log.error("Falha ao gerar a exportação {} ({})", exportId, export.type(), ex);
            fail(companyId, export, ex);
        }
    }

    private void fail(UUID companyId, ReportExport export, RuntimeException cause) {
        try {
            if (export.status() == ReportExportStatus.PENDING) {
                export.markFailed(cause.getMessage());
                inTenant(companyId, () -> {
                    repository.update(export);
                    return null;
                });
            }
            notifications.create(companyId, new CreateNotificationRequest(export.requestedBy(),
                    NotificationType.SYSTEM, "Não foi possível gerar o relatório",
                    summary(export) + ". Tente novamente em instantes.", null), export.requestedBy());
        } catch (RuntimeException ex) {
            log.error("Falha ao registrar o erro da exportação {}", export.id(), ex);
        }
    }

    private byte[] render(ReportExport export) {
        UUID companyId = export.companyId();
        LocalDate from = export.periodFrom();
        LocalDate to = export.periodTo();
        CompanyHeader company = dataGateway.companyHeader(companyId);
        LocalDateTime issuedAt = LocalDateTime.now(clock).withNano(0);
        ReportDocument document = switch (export.type()) {
            case MONTHLY_DRE -> ReportDocumentFactory.monthlyDre(datasets.monthlyDre(companyId, from, to),
                    YearMonth.from(from), company, issuedAt);
            case CASH_FLOW -> ReportDocumentFactory.cashFlow(datasets.cashFlow(companyId, from, to), from, to,
                    company, issuedAt);
            case TRANSACTIONS -> ReportDocumentFactory.transactions(datasets.transactions(companyId, from, to),
                    from, to, company, issuedAt);
            case AGENDA -> ReportDocumentFactory.agenda(datasets.agenda(companyId, from, to), from, to, company,
                    issuedAt);
        };
        return export.format() == ExportFormat.PDF
                ? PdfReportLayout.render(document)
                : XlsxReportWriter.write(document);
    }

    /** {@code contabil-mensal_2026-06.pdf} ou {@code agenda_2026-06-01_a_2026-06-30.xlsx}. */
    static String fileName(ReportExport export) {
        String period = switch (export.type()) {
            case MONTHLY_DRE -> YearMonth.from(export.periodFrom()).toString();
            default -> export.periodFrom() + "_a_" + export.periodTo();
        };
        return export.type().fileSlug() + "_" + period + "." + export.format().extension();
    }

    static String summary(ReportExport export) {
        String period = switch (export.type()) {
            case MONTHLY_DRE -> FinanceLabels.monthLabel(YearMonth.from(export.periodFrom()));
            default -> FinanceLabels.dateLabel(export.periodFrom()) + " a "
                    + FinanceLabels.dateLabel(export.periodTo());
        };
        return export.type().label() + " · " + period + " · " + export.format().label();
    }

    private String metadata(ReportExport export) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("kind", NOTIFICATION_KIND);
        data.put("exportId", export.id().toString());
        data.put("fileName", export.fileName());
        data.put("format", export.format().name());
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static <T> T inTenant(UUID companyId, Supplier<T> action) {
        try {
            TenantContext.setCompanyId(companyId);
            return action.get();
        } finally {
            TenantContext.clear();
        }
    }
}
