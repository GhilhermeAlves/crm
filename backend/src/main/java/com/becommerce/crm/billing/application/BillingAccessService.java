package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** "A empresa está bloqueada pela cobrança?" — consultado a cada requisição, por isso o cache de 60 s. */
@Service
public class BillingAccessService {

    static final long TTL_SECONDS = 60;

    private record Entry(boolean blocked, Instant at) {}

    private final BillingAccountJpaRepository accounts;
    private final Clock clock;
    private final ConcurrentHashMap<UUID, Entry> cache = new ConcurrentHashMap<>();

    public BillingAccessService(BillingAccountJpaRepository accounts, Clock clock) {
        this.accounts = accounts;
        this.clock = clock;
    }

    public boolean isBlocked(UUID companyId) {
        Instant now = clock.instant();
        Entry e = cache.get(companyId);
        if (e != null && e.at().plusSeconds(TTL_SECONDS).isAfter(now)) return e.blocked();
        boolean blocked = BillingTenant.run(companyId, () -> accounts.findById(companyId)
                .map(a -> a.getStatus() == BillingStatus.BLOCKED)
                .orElse(false));
        cache.put(companyId, new Entry(blocked, now));
        return blocked;
    }

    public void evict(UUID companyId) { cache.remove(companyId); }
}
