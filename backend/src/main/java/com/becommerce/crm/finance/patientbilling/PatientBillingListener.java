package com.becommerce.crm.finance.patientbilling;

import com.becommerce.crm.clinical.budget.application.service.BudgetChargeRequestedEvent;
import com.becommerce.crm.finance.transaction.application.service.FinancialTransactionChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/** Liga o financeiro e o orçamento ao Asaas, sempre depois do commit (falha no Asaas não desfaz nada). */
@Component
public class PatientBillingListener {

    private static final Logger log = LoggerFactory.getLogger(PatientBillingListener.class);
    private final PatientBillingService service;

    public PatientBillingListener(PatientBillingService service) {
        this.service = service;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTransactionChanged(FinancialTransactionChangedEvent e) {
        try {
            switch (e.kind()) {
                case PAID -> service.onPaidInClinic(e.companyId(), e.transactionId());
                case CANCELLED, DELETED -> service.onCancelled(e.companyId(), e.transactionId());
            }
        } catch (RuntimeException ex) {
            log.warn("[PATIENT-BILLING] reflexo no Asaas de {} falhou: {}", e.transactionId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBudgetCharge(BudgetChargeRequestedEvent e) {
        for (UUID id : e.transactionIds()) {
            try {
                service.charge(e.companyId(), id);
            } catch (RuntimeException ex) {
                log.warn("[PATIENT-BILLING] cobrança da parcela {} não criada: {}", id, ex.getMessage());
            }
        }
    }
}
