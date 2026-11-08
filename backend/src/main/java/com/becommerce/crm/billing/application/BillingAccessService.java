package com.becommerce.crm.billing.application;

import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Responde "a empresa está bloqueada pela cobrança?" com cache curto. Completada na Task 5. */
@Service
public class BillingAccessService {
    final ConcurrentHashMap<UUID, Object> cache = new ConcurrentHashMap<>();

    public void evict(UUID companyId) { cache.remove(companyId); }
}
