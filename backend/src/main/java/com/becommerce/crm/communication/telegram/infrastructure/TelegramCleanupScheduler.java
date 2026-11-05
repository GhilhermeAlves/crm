package com.becommerce.crm.communication.telegram.infrastructure;

import com.becommerce.crm.communication.telegram.application.TelegramConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Limpeza diária de {@code telegram_update_seen} (idempotência de updates do Telegram).
 *
 * <p>As tabelas têm RLS FORCE: um DELETE sem tenant não apaga nada. Por isso a limpeza
 * percorre as empresas e apaga cada uma dentro do seu próprio tenant. A lista de empresas
 * vem da tabela {@code companies}, que não tem RLS (mesmo princípio do {@code CampaignScheduler},
 * que lista empresas sem tenant). Isso também cobre linhas órfãs de empresas cujo bot foi removido.
 */
@Component
public class TelegramCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TelegramCleanupScheduler.class);

    private final TelegramUpdateSeenJpaRepository seen;
    private final JdbcTemplate jdbc;

    public TelegramCleanupScheduler(TelegramUpdateSeenJpaRepository seen, JdbcTemplate jdbc) {
        this.seen = seen;
        this.jdbc = jdbc;
    }

    @Scheduled(cron = "${app.telegram.cleanup-cron:0 30 4 * * *}")
    public void purge() {
        LocalDateTime before = LocalDateTime.now().minusDays(2);
        List<UUID> companyIds = jdbc.queryForList("SELECT id FROM companies", UUID.class);
        int removed = 0;
        for (UUID companyId : companyIds) {
            try {
                removed += TelegramConfigService.withTenant(companyId,
                        () -> seen.deleteOlderThan(before));
            } catch (Exception e) {
                log.error("[TELEGRAM] falha na limpeza de updates vistos (company={}): {}",
                        companyId, e.getMessage(), e);
            }
        }
        log.info("[TELEGRAM] limpeza de updates vistos concluída: {} removidos", removed);
    }
}
