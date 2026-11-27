package com.becommerce.crm.reports.export.infrastructure.scheduler;

import com.becommerce.crm.masterdata.storage.application.port.input.StorageUseCase;
import com.becommerce.crm.masterdata.storage.domain.exception.StorageObjectNotFoundException;
import com.becommerce.crm.reports.export.application.port.out.ReportExportRepository;
import com.becommerce.crm.reports.export.domain.ReportExport;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Retenção de 7 dias das exportações de relatório. Diariamente apaga do storage
 * o arquivo das exportações READY finalizadas há mais de 7 dias e as marca
 * EXPIRED (o registro fica como histórico).
 *
 * <p>{@code report_exports} tem RLS FORCE: a varredura percorre as empresas
 * via {@code app.all_company_ids()} (mesmo padrão do
 * {@code TelegramCleanupScheduler}) e trata cada uma dentro do próprio tenant.
 * Falha em um item ou empresa não interrompe os demais.
 */
@Component
public class ReportExportRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(ReportExportRetentionJob.class);
    static final Duration RETENTION = Duration.ofDays(7);
    static final int BATCH = 500;

    private final ReportExportRepository repository;
    private final StorageUseCase storage;
    private final JdbcTemplate jdbc;

    public ReportExportRetentionJob(ReportExportRepository repository, StorageUseCase storage, JdbcTemplate jdbc) {
        this.repository = repository;
        this.storage = storage;
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "${app.reports.export-retention-cron:0 15 3 * * *}")
    public void purge() {
        Instant before = Instant.now().minus(RETENTION);
        List<UUID> companyIds = jdbc.queryForList("SELECT id FROM app.all_company_ids()", UUID.class);
        int expired = 0;
        for (UUID companyId : companyIds) {
            try {
                expired += purgeCompany(companyId, before);
            } catch (Exception e) {
                log.error("[RELATORIOS] falha no expurgo de exportações (company={}): {}",
                        companyId, e.getClass().getSimpleName());
            } finally {
                TenantContext.clear();
            }
        }
        log.info("[RELATORIOS] expurgo de exportações concluído: {} expiradas", expired);
    }

    int purgeCompany(UUID companyId, Instant before) {
        List<ReportExport> candidates;
        try {
            TenantContext.setCompanyId(companyId);
            candidates = repository.findReadyFinishedBefore(companyId, before, BATCH);
        } finally {
            TenantContext.clear();
        }
        int expired = 0;
        for (ReportExport export : candidates) {
            try {
                expireOne(companyId, export);
                expired++;
            } catch (Exception e) {
                log.warn("[RELATORIOS] falha ao expirar exportação {} (company={}): {}",
                        export.id(), companyId, e.getClass().getSimpleName());
            } finally {
                TenantContext.clear();
            }
        }
        return expired;
    }

    private void expireOne(UUID companyId, ReportExport export) {
        if (export.storageObjectId() != null) {
            try {
                // O StorageService seta e limpa o TenantContext por conta própria.
                storage.delete(companyId, export.storageObjectId());
            } catch (StorageObjectNotFoundException ignored) {
                // Arquivo já removido: basta marcar como expirada.
            }
        }
        export.markExpired();
        TenantContext.setCompanyId(companyId);
        repository.update(export);
    }
}
