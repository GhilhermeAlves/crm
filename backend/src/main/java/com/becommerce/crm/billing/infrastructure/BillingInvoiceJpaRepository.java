package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.domain.BillingInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BillingInvoiceJpaRepository extends JpaRepository<BillingInvoice, UUID> {
    List<BillingInvoice> findTop24ByCompanyIdOrderByDueDateDesc(UUID companyId);
}
