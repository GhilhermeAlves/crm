package com.becommerce.crm.billing.application;

import com.becommerce.crm.shared.tenant.context.TenantContext;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * As tabelas billing_* têm RLS FORCE e o TenantAwareDataSource aplica o tenant por statement.
 * Toda leitura/escrita da cobrança roda aqui dentro, com saveAndFlush, para que o SQL saia com o
 * tenant da empresa alvo — mesmo vindo do webhook, do job ou de um SUPER_ADMIN logado em outra empresa.
 */
public final class BillingTenant {
    private BillingTenant() {}

    public static <T> T run(UUID companyId, Supplier<T> work) {
        UUID previous = TenantContext.getCompanyId();
        boolean swap = previous == null || !previous.equals(companyId);
        try {
            if (swap) TenantContext.setCompanyId(companyId);
            return work.get();
        } finally {
            if (swap) {
                if (previous == null) TenantContext.clear(); else TenantContext.setCompanyId(previous);
            }
        }
    }
}
