package com.becommerce.crm.finance.accounting.application.port.output;

import com.becommerce.crm.finance.accounting.domain.AccountingReportType;
import com.becommerce.crm.finance.accounting.domain.ReportFormat;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Registro das solicitações de relatório contábil ({@code accounting_report_requests},
 * V093). Se o mesmo (empresa, tipo, formato, mês) já existir, incrementa
 * {@code generated_count} em vez de bloquear — a regra da tela é de auditoria,
 * não de bloqueio (o arquivo é gerado de novo).
 */
public interface AccountingReportRequestRepository {

    /**
     * Registra (ou incrementa) a solicitação e devolve o valor atualizado de
     * {@code generated_count}.
     */
    int register(UUID companyId, AccountingReportType type, ReportFormat format, YearMonth month,
                 UUID requestedBy);
}