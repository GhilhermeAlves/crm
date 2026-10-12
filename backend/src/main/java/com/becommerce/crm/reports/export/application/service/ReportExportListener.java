package com.becommerce.crm.reports.export.application.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Gera o arquivo em segundo plano, só depois que o pedido PENDING foi commitado. */
@Component
public class ReportExportListener {

    private final ReportExportGenerator generator;

    public ReportExportListener(ReportExportGenerator generator) {
        this.generator = generator;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRequested(ReportExportRequestedEvent event) {
        generator.generate(event.companyId(), event.exportId());
    }
}
