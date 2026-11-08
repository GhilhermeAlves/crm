package com.becommerce.crm.billing.infrastructure;

import com.becommerce.crm.billing.domain.BillingInvoice;
import com.becommerce.crm.billing.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BillingInvoiceJpaRepository extends JpaRepository<BillingInvoice, UUID> {
    List<BillingInvoice> findTop24ByCompanyIdOrderByDueDateDesc(UUID companyId);

    Optional<BillingInvoice> findByAsaasPaymentId(String asaasPaymentId);

    Optional<BillingInvoice> findFirstByCompanyIdAndStatusOrderByDueDateAsc(UUID companyId, InvoiceStatus status);

    List<BillingInvoice> findByCompanyIdAndStatusIn(UUID companyId, Collection<InvoiceStatus> statuses);

    List<BillingInvoice> findByCompanyIdAndStatusInAndDueDateBefore(UUID companyId,
                                                                    Collection<InvoiceStatus> statuses,
                                                                    LocalDate date);
}
