package com.becommerce.crm.billing.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BillingInvoiceTest {
    @Test
    void fromAsaasAndRefresh() {
        UUID company = UUID.randomUUID();
        BillingInvoice i = BillingInvoice.fromAsaas(company, "pay_1", LocalDate.of(2026, 11, 15), 19700,
                InvoiceStatus.PENDING, "https://pix/1", null);
        assertNotNull(i.getId());
        assertEquals(company, i.getCompanyId());
        assertTrue(i.isOpen());

        LocalDateTime paid = LocalDateTime.of(2026, 11, 14, 10, 0);
        i.refresh(LocalDate.of(2026, 11, 15), 19700, InvoiceStatus.RECEIVED, "https://pix/1", paid);
        assertEquals(InvoiceStatus.RECEIVED, i.getStatus());
        assertEquals(paid, i.getPaidAt());
        assertFalse(i.isOpen());
    }
}
