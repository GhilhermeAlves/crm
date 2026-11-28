package com.becommerce.crm.reports.export.infrastructure.scheduler;

import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.application.service.ReportExportGenerator;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Gerações travadas: exportações PENDING criadas há mais de N minutos (padrão
 * 30) viram FAILED com orientação ao usuário e a notificação de falha de
 * sempre. Se o gerador terminar depois, a gravação condicional dele não pega
 * e o arquivo é descartado.
 *
 * <p>Mesmo padrão de varredura do {@link ReportExportRetentionJob}: percorre
 * as empresas via {@code app.all_company_ids()} e opera no tenant de cada uma.
 */
@Component
public class ReportExportStuckJob {

    static final String TIMEOUT_MESSAGE = "Tempo de geração excedido. Tente novamente.";
    static final int BATCH = 500;

    private static final Logger log = LoggerFactory.getLogger(ReportExportStuckJob.class);

    private final ReportExportRepository repository;
    private final ReportExportGenerator generator;
    private final JdbcTemplate jdbc;
    private final Duration timeout;

    public ReportExportStuckJob(ReportExportRepository repository, ReportExportGenerator generator,
                                JdbcTemplate jdbc,
                                @Value("${app.reports.export-stuck-minutes:30}") long stuckMinutes) {
        this.repository = repository;
        this.generator = generator;
        this.jdbc = jdbc;
        this.timeout = Duration.ofMinutes(stuckMinutes);
    }

    @Scheduled(cron = "${app.reports.export-stuck-cron:0 */10 * * * *}")
    public void failStuck() {
        Instant before = Instant.now().minus(timeout);
        List<UUID> companyIds = jdbc.queryForList("SELECT id FROM app.all_company_ids()", UUID.class);
        int failed = 0;
        for (UUID companyId : companyIds) {
            try {
                failed += failCompany(companyId, before);
            } catch (Exception e) {
                log.error("[RELATORIOS] falha na varredura de exportações travadas (company={}): {}",
                        companyId, e.getClass().getSimpleName());
            } finally {
                TenantContext.clear();
            }
        }
        if (failed > 0) {
            log.info("[RELATORIOS] {} exportações travadas marcadas como falha", failed);
        }
    }

    int failCompany(UUID companyId, Instant before) {
        List<ReportExport> stuck;
        try {
            TenantContext.setCompanyId(companyId);
            stuck = repository.findPendingCreatedBefore(companyId, before, BATCH);
        } finally {
            TenantContext.clear();
        }
        int failed = 0;
        for (ReportExport export : stuck) {
            try {
                if (generator.failPending(companyId, export, TIMEOUT_MESSAGE)) {
                    failed++;
                }
            } catch (Exception e) {
                log.warn("[RELATORIOS] falha ao encerrar exportação travada {} (company={}): {}",
                        export.id(), companyId, e.getClass().getSimpleName());
            } finally {
                TenantContext.clear();
            }
        }
        return failed;
    }
}
