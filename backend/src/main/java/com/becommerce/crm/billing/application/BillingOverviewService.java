package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Status de cobrança de todas as empresas (coluna da lista do SUPER_ADMIN). */
@Service
public class BillingOverviewService {

    private static final Logger log = LoggerFactory.getLogger(BillingOverviewService.class);

    public record CompanyBillingStatus(UUID companyId, String status) {}

    private final JdbcTemplate jdbc;
    private final BillingAccountJpaRepository accounts;

    public BillingOverviewService(JdbcTemplate jdbc, BillingAccountJpaRepository accounts) {
        this.jdbc = jdbc;
        this.accounts = accounts;
    }

    public List<CompanyBillingStatus> statuses() {
        List<UUID> ids = jdbc.queryForList("SELECT id FROM companies ORDER BY created_at", UUID.class);
        List<CompanyBillingStatus> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            String status;
            try {
                status = BillingTenant.run(id, () -> accounts.findById(id)
                        .map(a -> a.getStatus().name())
                        .orElse("NOT_STARTED"));
            } catch (RuntimeException e) {
                log.warn("[BILLING] status da empresa {} indisponível: {}", id, e.getMessage());
                status = "UNKNOWN";
            }
            result.add(new CompanyBillingStatus(id, status));
        }
        return result;
    }
}
