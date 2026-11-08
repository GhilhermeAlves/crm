package com.becommerce.crm.billing.application;

import com.becommerce.crm.billing.domain.BillingAccount;
import com.becommerce.crm.billing.domain.BillingStatus;
import com.becommerce.crm.billing.infrastructure.AsaasClient;
import com.becommerce.crm.billing.infrastructure.BillingAccountJpaRepository;
import com.becommerce.crm.masterdata.company.application.port.output.CompanyRepository;
import com.becommerce.crm.masterdata.company.domain.Company;
import com.becommerce.crm.masterdata.company.domain.CompanyNotFoundException;
import com.becommerce.crm.masterdata.company.domain.CompanyPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    static final int FIRST_DUE_DAYS = 7;

    private final BillingAccountJpaRepository accounts;
    private final CompanyRepository companies;
    private final AsaasClient asaas;
    private final BillingProperties props;
    private final Clock clock;

    public BillingService(BillingAccountJpaRepository accounts, CompanyRepository companies,
                          AsaasClient asaas, BillingProperties props, Clock clock) {
        this.accounts = accounts;
        this.companies = companies;
        this.asaas = asaas;
        this.props = props;
        this.clock = clock;
    }

    public record BillingView(String status, String plan, int monthlyPriceCents, LocalDate nextDueDate,
                              String syncError, boolean hasSubscription) {
        static BillingView of(BillingAccount a) {
            return new BillingView(a.getStatus().name(), a.getPlan().name(), a.getMonthlyPriceCents(),
                    a.getNextDueDate(), a.getSyncError(), a.hasSubscription());
        }
        static BillingView notStarted(CompanyPlan plan) {
            return new BillingView(BillingStatus.NOT_STARTED.name(), plan.name(),
                    plan.defaultMonthlyPriceCents(), null, null, false);
        }
    }

    @Transactional(readOnly = true)
    public BillingView view(UUID companyId) {
        Company company = requireCompany(companyId);
        return inTenant(companyId, () -> accounts.findById(companyId)
                .map(BillingView::of)
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
                return BillingView.of(acc);
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
            return BillingView.of(accounts.saveAndFlush(acc));
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
            return BillingView.of(accounts.saveAndFlush(acc));
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
            return BillingView.of(accounts.saveAndFlush(acc));
        });
    }

    @Transactional
    public void onPlanChanged(UUID companyId, CompanyPlan newPlan) {
        inTenant(companyId, () -> {
            accounts.findById(companyId).ifPresent(acc -> {
                if (acc.getPlan() == newPlan) return;
                boolean defaultPrice = acc.getMonthlyPriceCents() == acc.getPlan().defaultMonthlyPriceCents();
                int cents = defaultPrice ? newPlan.defaultMonthlyPriceCents() : acc.getMonthlyPriceCents();
                acc.changePrice(newPlan, cents);
                if (defaultPrice) pushPrice(acc);
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
        return companies.findById(companyId).orElseThrow(() -> new CompanyNotFoundException(companyId));
    }

    private <T> T inTenant(UUID companyId, Supplier<T> work) {
        return BillingTenant.run(companyId, work);
    }
}
