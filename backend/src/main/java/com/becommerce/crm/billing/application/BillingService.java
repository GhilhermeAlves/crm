package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingInvoice;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.billing.infrastructure.BillingInvoiceJpaRepository;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyNotFoundException;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    static final int FIRST_DUE_DAYS = 7;

    private final BillingAccountJpaRepository accounts;
    private final BillingInvoiceJpaRepository invoices;
    private final CompanyRepository companies;
    private final AsaasClient asaas;
    private final BillingPaymentService payments;
    private final BillingAccessService access;
    private final BillingProperties props;
    private final Clock clock;

    public BillingService(BillingAccountJpaRepository accounts, BillingInvoiceJpaRepository invoices,
                          CompanyRepository companies, AsaasClient asaas, BillingPaymentService payments,
                          BillingAccessService access, BillingProperties props, Clock clock) {
        this.accounts = accounts;
        this.invoices = invoices;
        this.companies = companies;
        this.asaas = asaas;
        this.payments = payments;
        this.access = access;
        this.props = props;
        this.clock = clock;
    }

    public record BillingView(String status, String plan, int monthlyPriceCents, LocalDate nextDueDate,
                              String syncError, boolean hasSubscription, LocalDate overdueSince,
                              LocalDate graceUntil, String openInvoiceUrl) {
        static BillingView of(BillingAccount a, String openInvoiceUrl) {
            return new BillingView(a.getStatus().name(), a.getPlan().name(), a.getMonthlyPriceCents(),
                    a.getNextDueDate(), a.getSyncError(), a.hasSubscription(), a.getOverdueSince(),
                    a.getGraceUntil(), openInvoiceUrl);
        }
        static BillingView notStarted(CompanyPlan plan) {
            return new BillingView(BillingStatus.NOT_STARTED.name(), plan.name(),
                    plan.defaultMonthlyPriceCents(), null, null, false, null, null, null);
        }
    }

    @Transactional(readOnly = true)
    public BillingView view(UUID companyId) {
        Company company = requireCompany(companyId);
        return inTenant(companyId, () -> accounts.findById(companyId)
                .map(this::viewOf)
                .orElseGet(() -> BillingView.notStarted(company.getPlan())));
    }

    @Transactional
    public BillingView activate(UUID companyId) {
        if (!props.configured()) {
            throw new BillingRuleException("Cobrança desligada: configure ASAAS_API_KEY e BILLING_ENABLED.");
        }
        Company company = requireCompany(companyId);
        if (company.getCnpj() == null || company.getCnpj().isBlank()) {
            throw new BillingRuleException("Cadastre o CPF ou CNPJ da empresa antes de ativar a cobrança.");
        }
        return inTenant(companyId, () -> {
            BillingAccount acc = accounts.findById(companyId).orElseGet(() -> BillingAccount.start(
                    companyId, company.getPlan(), company.getPlan().defaultMonthlyPriceCents()));
            if (acc.getStatus() == BillingStatus.EXEMPT) {
                throw new BillingRuleException("Empresa isenta não é cobrada.");
            }
            if (acc.hasSubscription()) {
                return viewOf(acc);
            }
            String ref = companyId.toString();
            try {
                if (acc.getAsaasCustomerId() == null) {
                    acc.customerCreated(asaas.createCustomer(
                            company.getTradingName(), company.getCnpj(), company.getEmail(), ref));
                }
                LocalDate firstDue = LocalDate.now(clock).plusDays(FIRST_DUE_DAYS);
                String sub = asaas.createSubscription(acc.getAsaasCustomerId(), acc.getMonthlyPriceCents(),
                        firstDue, description(acc.getPlan()), ref);
                acc.activated(acc.getAsaasCustomerId(), sub, firstDue);
            } catch (AsaasClient.AsaasException e) {
                log.warn("Falha ao ativar cobrança da empresa {}: {}", companyId, e.getMessage());
                acc.syncFailed(e.getMessage());
            }
            return viewOf(accounts.saveAndFlush(acc));
        });
    }

    @Transactional
    public BillingView markExempt(UUID companyId) {
        Company company = requireCompany(companyId);
        return inTenant(companyId, () -> {
            BillingAccount acc = accounts.findById(companyId).orElseGet(() -> BillingAccount.start(
                    companyId, company.getPlan(), company.getPlan().defaultMonthlyPriceCents()));
            if (acc.hasSubscription()) {
                throw new BillingRuleException(
                        "Empresa com assinatura ativa no Asaas: cancele a assinatura antes de isentar.");
            }
            acc.markExempt();
            return viewOf(accounts.saveAndFlush(acc));
        });
    }

    @Transactional
    public BillingView changePrice(UUID companyId, int cents) {
        Company company = requireCompany(companyId);
        return inTenant(companyId, () -> {
            BillingAccount acc = accounts.findById(companyId).orElseGet(() -> BillingAccount.start(
                    companyId, company.getPlan(), company.getPlan().defaultMonthlyPriceCents()));
            try {
                acc.changePrice(acc.getPlan(), cents);
            } catch (IllegalArgumentException e) {
                throw new BillingRuleException(e.getMessage());
            }
            pushPrice(acc);
            return viewOf(accounts.saveAndFlush(acc));
        });
    }

    // REQUIRES_NEW: é chamado no AFTER_COMMIT da edição da empresa, onde a transação original já terminou.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPlanChanged(UUID companyId, CompanyPlan newPlan) {
        inTenant(companyId, () -> {
            accounts.findById(companyId).ifPresent(acc -> {
                if (acc.getPlan() == newPlan) return;
                // Regra comercial: trocar de plano sempre aplica o preço do plano novo
                // (um valor negociado pode ser definido de novo no cartão depois).
                acc.changePrice(newPlan, newPlan.defaultMonthlyPriceCents());
                pushPrice(acc);
                accounts.saveAndFlush(acc);
            });
            return null;
        });
    }

    @Transactional
    public void retrySync(UUID companyId) {
        BillingAccount acc = inTenant(companyId, () -> accounts.findById(companyId).orElse(null));
        if (acc == null) return;
        if (acc.getStatus() == BillingStatus.SYNC_PENDING) {
            try {
                activate(companyId);
            } catch (BillingRuleException e) {
                log.info("Reenvio da cobrança de {} não feito: {}", companyId, e.getMessage());
            }
            return;
        }
        if (acc.isPriceSyncPending()) {
            inTenant(companyId, () -> {
                pushPrice(acc);
                return accounts.saveAndFlush(acc);
            });
        }
    }

    public record InvoiceView(UUID id, LocalDate dueDate, int valueCents, String status, String invoiceUrl,
                              LocalDateTime paidAt) {
        static InvoiceView of(BillingInvoice i) {
            return new InvoiceView(i.getId(), i.getDueDate(), i.getValueCents(), i.getStatus().name(),
                    i.getInvoiceUrl(), i.getPaidAt());
        }
    }

    static final int MAX_GRANT_DAYS = 30;

    // Sem @Transactional: o evict precisa rodar depois do commit do saveAndFlush.
    public BillingView grant(UUID companyId, int days) {
        if (days < 1 || days > MAX_GRANT_DAYS) throw new BillingRuleException("Libere entre 1 e 30 dias.");
        BillingView view = inTenant(companyId, () -> {
            BillingAccount acc = accounts.findById(companyId)
                    .orElseThrow(() -> new BillingRuleException("Empresa sem cobrança ativa."));
            acc.grant(LocalDate.now(clock).plusDays(days));
            return viewOf(accounts.saveAndFlush(acc));
        });
        access.evict(companyId);
        return view;
    }

    @Transactional(readOnly = true)
    public List<InvoiceView> invoices(UUID companyId) {
        return inTenant(companyId, () -> invoices.findTop24ByCompanyIdOrderByDueDateDesc(companyId).stream()
                .map(InvoiceView::of).toList());
    }

    public BillingView checkPayment(UUID companyId) {
        if (props.configured()) {
            payments.refreshOpen(companyId, null);
            access.evict(companyId);
        }
        return view(companyId);
    }

    /** View com a URL Pix da fatura aberta mais antiga (para a faixa e a tela de pagamento pendente). */
    private BillingView viewOf(BillingAccount a) {
        String url = invoices.findByCompanyIdAndStatusIn(a.getCompanyId(), BillingPaymentService.OPEN).stream()
                .min(java.util.Comparator.comparing(BillingInvoice::getDueDate))
                .map(BillingInvoice::getInvoiceUrl).orElse(null);
        return BillingView.of(a, url);
    }

    private void pushPrice(BillingAccount acc) {
        if (!acc.hasSubscription()) return;
        try {
            asaas.updateSubscriptionValue(acc.getAsaasSubscriptionId(), acc.getMonthlyPriceCents(),
                    description(acc.getPlan()));
            acc.priceSynced();
        } catch (AsaasClient.AsaasException e) {
            log.warn("Falha ao atualizar valor no Asaas da empresa {}: {}", acc.getCompanyId(), e.getMessage());
            acc.priceSyncFailed(e.getMessage());
        }
    }

    private static String description(CompanyPlan plan) {
        return "Mensalidade CRM - plano " + plan.name().charAt(0) + plan.name().substring(1).toLowerCase();
    }

    private Company requireCompany(UUID companyId) {
        return BillingTenant.run(companyId, () -> companies.findById(companyId))
                .orElseThrow(() -> new CompanyNotFoundException(companyId));
    }

    private <T> T inTenant(UUID companyId, Supplier<T> work) {
        return BillingTenant.run(companyId, work);
    }
}
