package com.becommerce.crm.finance.patientbilling;

import com.becommerce.crm.billing.application.BillingTenant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência da cobrança de pacientes (V141). Tabelas com RLS FORCE: tudo roda no tenant
 * da empresa alvo via {@link BillingTenant} (o TenantAwareDataSource aplica por statement).
 */
@Component
public class PatientChargeStore {

    public record PatientCharge(UUID transactionId, UUID companyId, String asaasPaymentId, String invoiceUrl,
                                String status, String error) {}

    private final JdbcTemplate jdbc;

    public PatientChargeStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<PatientCharge> find(UUID companyId, UUID transactionId) {
        return BillingTenant.run(companyId, () -> jdbc.query(
                "SELECT transaction_id, company_id, asaas_payment_id, invoice_url, status, error "
                        + "FROM patient_charges WHERE company_id = ? AND transaction_id = ?",
                (rs, i) -> map(rs), companyId, transactionId).stream().findFirst());
    }

    public List<PatientCharge> findAll(UUID companyId, Collection<UUID> transactionIds) {
        if (transactionIds.isEmpty()) return List.of();
        String marks = String.join(",", java.util.Collections.nCopies(transactionIds.size(), "?"));
        Object[] args = new Object[transactionIds.size() + 1];
        args[0] = companyId;
        int i = 1;
        for (UUID id : transactionIds) args[i++] = id;
        return BillingTenant.run(companyId, () -> jdbc.query(
                "SELECT transaction_id, company_id, asaas_payment_id, invoice_url, status, error "
                        + "FROM patient_charges WHERE company_id = ? AND transaction_id IN (" + marks + ")",
                (rs, n) -> map(rs), args));
    }

    public void upsert(PatientCharge c) {
        BillingTenant.run(c.companyId(), () -> jdbc.update(
                "INSERT INTO patient_charges (transaction_id, company_id, asaas_payment_id, invoice_url, status, error) "
                        + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (transaction_id) DO UPDATE SET "
                        + "asaas_payment_id = EXCLUDED.asaas_payment_id, invoice_url = EXCLUDED.invoice_url, "
                        + "status = EXCLUDED.status, error = EXCLUDED.error, updated_at = now()",
                c.transactionId(), c.companyId(), c.asaasPaymentId(), c.invoiceUrl(), c.status(),
                truncate(c.error())));
    }

    public void setStatus(UUID companyId, UUID transactionId, String status, String error) {
        BillingTenant.run(companyId, () -> jdbc.update(
                "UPDATE patient_charges SET status = ?, error = ?, updated_at = now() "
                        + "WHERE company_id = ? AND transaction_id = ?",
                status, truncate(error), companyId, transactionId));
    }

    public Optional<String> customerId(UUID companyId, UUID contactId) {
        return BillingTenant.run(companyId, () -> jdbc.queryForList(
                "SELECT asaas_customer_id FROM patient_billing_customers WHERE company_id = ? AND contact_id = ?",
                String.class, companyId, contactId).stream().findFirst());
    }

    public void saveCustomer(UUID companyId, UUID contactId, String asaasCustomerId) {
        BillingTenant.run(companyId, () -> jdbc.update(
                "INSERT INTO patient_billing_customers (contact_id, company_id, asaas_customer_id) VALUES (?, ?, ?) "
                        + "ON CONFLICT (contact_id) DO UPDATE SET asaas_customer_id = EXCLUDED.asaas_customer_id",
                contactId, companyId, asaasCustomerId));
    }

    public boolean enabled(UUID companyId) {
        return BillingTenant.run(companyId, () -> jdbc.queryForList(
                "SELECT enabled FROM patient_billing_companies WHERE company_id = ?", Boolean.class, companyId)
                .stream().findFirst().orElse(false));
    }

    public void setEnabled(UUID companyId, boolean on) {
        BillingTenant.run(companyId, () -> jdbc.update(
                "INSERT INTO patient_billing_companies (company_id, enabled) VALUES (?, ?) "
                        + "ON CONFLICT (company_id) DO UPDATE SET enabled = EXCLUDED.enabled, updated_at = now()",
                companyId, on));
    }

    private static PatientCharge map(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new PatientCharge(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6));
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
