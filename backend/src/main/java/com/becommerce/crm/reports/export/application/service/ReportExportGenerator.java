package com.becommerce.crm.reports.export.application.service;

import com.becommerce.crm.communication.notification.application.dto.CreateNotificationRequest;
import com.becommerce.crm.communication.notification.application.port.input.NotificationUseCase;
import com.becommerce.crm.communication.notification.domain.NotificationType;
import com.becommerce.crm.finance.accounting.application.service.FinanceLabels;
import com.becommerce.crm.masterdata.storage.application.dto.StorageResponse;
import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.reports.export.application.dto.AgendaDataset;
import com.becommerce.crm.reports.export.application.dto.BudgetsDataset;
import com.becommerce.crm.reports.export.application.dto.LeadsDataset;
import com.becommerce.crm.reports.export.application.dto.CashFlowDataset;
import com.becommerce.crm.reports.export.application.dto.MonthlyDreDataset;
import com.becommerce.crm.reports.export.application.dto.TransactionsDataset;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway;
import com.becommerce.crm.reports.export.application.port.out.ReportDataGateway.CompanyHeader;
import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.application.service.render.CsvReportWriter;
import com.becommerce.crm.reports.export.application.service.render.PdfReportLayout;
import com.becommerce.crm.reports.export.application.service.render.ReportDocument;
import com.becommerce.crm.reports.export.application.service.render.ReportDocumentFactory;
import com.becommerce.crm.reports.export.application.service.render.ReportFieldMasker;
import com.becommerce.crm.reports.export.application.service.render.XlsxReportWriter;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.reports.export.domain.ReportExportStatus;
import com.becommerce.crm.reports.export.domain.exception.ReportExportTooLargeException;
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
 * marca FAILED e avisa o usuário. Se o autor cancelar durante a geração, o
 * arquivo é descartado (apagado do storage se já subiu) e ninguém é avisado.
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
    private final ReportExportAuditor auditor;
    private Clock clock = Clock.system(CLINIC_ZONE);

    public ReportExportGenerator(ReportExportRepository repository, ReportDataGateway dataGateway,
                                 ReportDatasetBuilder datasets, StorageUseCase storage,
                                 NotificationUseCase notifications, ObjectMapper objectMapper,
                                 ReportExportAuditor auditor) {
        this.repository = repository;
        this.dataGateway = dataGateway;
        this.datasets = datasets;
        this.storage = storage;
        this.notifications = notifications;
        this.objectMapper = objectMapper;
        this.auditor = auditor;
    }

    /** Arquivo renderizado e a quantidade de linhas de dados do relatório. */
    private record Rendered(byte[] content, int rowCount) {
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
            Rendered rendered = inTenant(companyId, () -> render(export));
            // Cancelada (ou finalizada pelo job de travados) durante a geração: nem sobe o arquivo.
            if (!stillPending(companyId, exportId)) {
                log.info("Exportação {} finalizada durante a geração; arquivo descartado", exportId);
                return;
            }
            String fileName = fileName(export);
            // Arquivo temporário: não consome a cota de armazenamento da empresa.
            StorageResponse stored = storage.uploadTemporary(companyId, fileName, export.format().contentType(),
                    rendered.content(), export.requestedBy());
            export.markReady(stored.id(), fileName, rendered.rowCount());
            // Gravação condicional: se cancelaram entre o upload e aqui, apaga o objeto e não avisa.
            boolean published = inTenant(companyId, () -> repository.updateIfPending(export));
            if (!published) {
                log.info("Exportação {} cancelada antes de publicar; arquivo descartado", exportId);
                discard(companyId, stored.id());
                return;
            }
            auditor.record(ReportExportAuditor.Event.READY, export);
            notifications.create(companyId, new CreateNotificationRequest(export.requestedBy(),
                    NotificationType.SYSTEM, "Relatório pronto para download", summary(export),
                    metadata(export)), export.requestedBy());
        } catch (RuntimeException ex) {
            // Só ids e o tipo da exceção: a mensagem/stack pode carregar dados do relatório.
            log.error("[RELATORIOS] falha ao gerar a exportação {} ({}): {}", exportId, export.type(),
                    ex.getClass().getSimpleName());
            fail(companyId, export, ex);
        }
    }

    /**
     * Marca FAILED uma exportação PENDING com a mensagem ao usuário e envia a
     * notificação de falha (usado pelo job de gerações travadas). Retorna
     * {@code false} se outro fluxo já a finalizou.
     */
    public boolean failPending(UUID companyId, ReportExport export, String message) {
        export.markFailed(message);
        boolean updated = inTenant(companyId, () -> repository.updateIfPending(export));
        if (updated) {
            auditor.record(ReportExportAuditor.Event.FAILED, export);
            notifyFailure(companyId, export, message);
        }
        return updated;
    }

    private boolean stillPending(UUID companyId, UUID exportId) {
        return inTenant(companyId, () -> repository.findById(companyId, exportId))
                .map(current -> current.status() == ReportExportStatus.PENDING)
                .orElse(false);
    }

    private void discard(UUID companyId, UUID storageObjectId) {
        try {
            // O StorageService seta e limpa o TenantContext por conta própria.
            storage.delete(companyId, storageObjectId);
        } catch (RuntimeException ex) {
            log.warn("Falha ao apagar o arquivo descartado {}: {}", storageObjectId, ex.getClass().getSimpleName());
        }
    }

    private void fail(UUID companyId, ReportExport export, RuntimeException cause) {
        try {
            if (export.status() != ReportExportStatus.PENDING) {
                // Já marcada READY em memória mas não publicada: nada a registrar.
                return;
            }
            export.markFailed(cause.getMessage());
            boolean updated = inTenant(companyId, () -> repository.updateIfPending(export));
            if (!updated) {
                // Cancelada pelo autor (ou finalizada por outro fluxo): sem notificação.
                return;
            }
            auditor.record(ReportExportAuditor.Event.FAILED, export);
            // Limite de linhas não se resolve tentando de novo: mostra a orientação.
            String hint = cause instanceof ReportExportTooLargeException
                    ? cause.getMessage() : "Tente novamente em instantes.";
            notifyFailure(companyId, export, hint);
        } catch (RuntimeException ex) {
            log.error("[RELATORIOS] falha ao registrar o erro da exportação {}: {}", export.id(),
                    ex.getClass().getSimpleName());
        }
    }

    private void notifyFailure(UUID companyId, ReportExport export, String hint) {
        notifications.create(companyId, new CreateNotificationRequest(export.requestedBy(),
                NotificationType.SYSTEM, "Não foi possível gerar o relatório",
                summary(export) + ". " + hint, null), export.requestedBy());
    }

    private Rendered render(ReportExport export) {
        UUID companyId = export.companyId();
        LocalDate from = export.periodFrom();
        LocalDate to = export.periodTo();
        CompanyHeader company = dataGateway.companyHeader(companyId);
        LocalDateTime issuedAt = LocalDateTime.now(clock).withNano(0);
        int[] rows = {0};
        ReportDocument document = switch (export.type()) {
            case MONTHLY_DRE -> {
                MonthlyDreDataset data = datasets.monthlyDre(companyId, from, to);
                rows[0] = data.groups().stream().mapToInt(g -> g.lines().size()).sum();
                yield emptyNotice(ReportDocumentFactory.monthlyDre(data, YearMonth.from(from), company, issuedAt),
                        data.groups().stream().flatMap(g -> g.lines().stream()).allMatch(l -> l.count() == 0));
            }
            case CASH_FLOW -> {
                CashFlowDataset data = datasets.cashFlow(companyId, from, to);
                rows[0] = data.days().size();
                yield emptyNotice(ReportDocumentFactory.cashFlow(data, from, to, company, issuedAt),
                        data.days().isEmpty());
            }
            case TRANSACTIONS -> {
                TransactionsDataset data = datasets.transactions(companyId, from, to);
                rows[0] = data.rows().size();
                yield emptyNotice(ReportDocumentFactory.transactions(data, from, to, company, issuedAt),
                        data.rows().isEmpty());
            }
            case AGENDA -> {
                AgendaDataset data = datasets.agenda(companyId, from, to);
                rows[0] = data.rows().size();
                yield emptyNotice(ReportDocumentFactory.agenda(data, from, to, company, issuedAt),
                        data.rows().isEmpty());
            }
            case BUDGETS -> {
                BudgetsDataset data = datasets.budgets(companyId, from, to);
                rows[0] = data.rows().size();
                yield emptyNotice(ReportDocumentFactory.budgets(data, from, to, company, issuedAt),
                        data.rows().isEmpty());
            }
            case LEADS -> {
                LeadsDataset data = datasets.leads(companyId, from, to);
                rows[0] = data.rows().size();
                yield emptyNotice(ReportDocumentFactory.leads(data, from, to, company, issuedAt),
                        data.rows().isEmpty());
            }
        };
        // Máscara LGPD antes de qualquer renderer: vale igual para PDF, XLSX e CSV.
        document = ReportFieldMasker.forExport(export.unmasked()).apply(document);
        byte[] content = switch (export.format()) {
            case PDF -> PdfReportLayout.render(document, company.logoUrl());
            case XLSX -> XlsxReportWriter.write(document);
            case CSV -> CsvReportWriter.write(document);
        };
        return new Rendered(content, rows[0]);
    }

    /** Sem registros o arquivo sai normalmente (READY), com o aviso de estado vazio. */
    private static ReportDocument emptyNotice(ReportDocument document, boolean empty) {
        return empty ? document.withNotice(ReportDocument.EMPTY_NOTICE) : document;
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
