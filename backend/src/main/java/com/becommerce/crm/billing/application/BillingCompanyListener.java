package com.becommerce.crm.billing.application;

import com.becommerce.crm.masterdata.company.domain.event.CompanyCreatedEvent;
import com.becommerce.crm.masterdata.company.domain.event.CompanyUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Liga a cobrança à criação/edição da empresa, depois do commit (falha no Asaas não desfaz nada). */
@Component
public class BillingCompanyListener {

    private static final Logger log = LoggerFactory.getLogger(BillingCompanyListener.class);

    private final BillingService service;
    private final BillingProperties props;

    public BillingCompanyListener(BillingService service, BillingProperties props) {
        this.service = service;
        this.props = props;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(CompanyCreatedEvent event) {
        if (!props.configured()) return;
        try {
            service.activate(event.companyId());
        } catch (BillingRuleException e) {
            log.info("Cobrança não ativada para {}: {}", event.companyId(), e.getMessage());
        }
    }

    // Síncrono (sem @Async): a resposta da edição só volta com o valor do plano novo já gravado,
    // assim a tela recarrega com o valor certo. Falha aqui não derruba a edição (já commitada).
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUpdated(CompanyUpdatedEvent event) {
        if (event.plan() == null) return;
        try {
            service.onPlanChanged(event.companyId(), event.plan());
        } catch (RuntimeException e) {
            log.warn("Falha ao aplicar o valor do plano novo da empresa {}: {}", event.companyId(), e.getMessage());
        }
    }
}
