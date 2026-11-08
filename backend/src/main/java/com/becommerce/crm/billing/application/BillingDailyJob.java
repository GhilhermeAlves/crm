package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * 1×/dia (03:30, depois do backup): reenvia ao Asaas o que ficou pendente, reconcilia faturas vencidas
 * (caso o webhook tenha se perdido), expira carências e bloqueia quem passou de 10 dias de atraso.
 */
@Component
public class BillingDailyJob {

    private static final Logger log = LoggerFactory.getLogger(BillingDailyJob.class);
    static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private final JdbcTemplate jdbc;
    private final BillingAccountJpaRepository accounts;
    private final BillingService billing;
    private final BillingPaymentService payments;
    private final BillingAccessService access;
    private final BillingProperties props;
    private final Clock clock;

    public BillingDailyJob(JdbcTemplate jdbc, BillingAccountJpaRepository accounts, BillingService billing,
                           BillingPaymentService payments, BillingAccessService access,
                           BillingProperties props, Clock clock) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.billing = billing;
        this.payments = payments;
        this.access = access;
        this.props = props;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.billing.daily-cron:0 30 3 * * *}", zone = "America/Sao_Paulo")
    public void run() {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        List<UUID> companyIds = jdbc.queryForList("SELECT id FROM companies", UUID.class);
        int blocked = 0;
        for (UUID companyId : companyIds) {
            try {
                if (process(companyId, today)) blocked++;
            } catch (Exception e) {
                log.error("[BILLING] job diário falhou para a empresa {}: {}", companyId, e.getMessage());
            }
        }
        log.info("[BILLING] job diário concluído: {} empresas, {} bloqueadas hoje", companyIds.size(), blocked);
    }

    private boolean process(UUID companyId, LocalDate today) {
        BillingAccount acc = BillingTenant.run(companyId, () -> accounts.findById(companyId).orElse(null));
        if (acc == null || acc.getStatus() == BillingStatus.EXEMPT || acc.getStatus() == BillingStatus.NOT_STARTED) {
            return false;
        }
        if (props.configured()) {
            billing.retrySync(companyId);
            payments.refreshOpen(companyId, today);
        }
        return BillingTenant.run(companyId, () -> {
            BillingAccount fresh = accounts.findById(companyId).orElseThrow();
            boolean before = fresh.getStatus() == BillingStatus.BLOCKED;
            fresh.expireGrace(today);
            if (fresh.shouldBlock(today)) fresh.block();
            accounts.saveAndFlush(fresh);
            access.evict(companyId);
            return !before && fresh.getStatus() == BillingStatus.BLOCKED;
        });
    }
}
