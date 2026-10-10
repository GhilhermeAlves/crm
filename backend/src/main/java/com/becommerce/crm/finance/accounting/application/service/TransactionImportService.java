package com.becommerce.crm.finance.accounting.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.finance.accounting.application.dto.CsvImportResult;
import com.becommerce.crm.finance.accounting.application.dto.CsvImportRow;
import com.becommerce.crm.finance.accounting.application.dto.CsvLineError;
import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportGateway;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.shared.tenant.context.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Importação de lançamentos via CSV (Etapa 3). Cada linha válida vira um
 * lançamento simples (1 parcela); linhas inválidas ou com paciente/profissional
 * não encontrados viram erros por linha sem abortar as demais. Persiste tudo em
 * uma transação via {@link FinancialTransactionRepository#saveAll} e registra um
 * único evento de auditoria por importação.
 */
@Service
public class TransactionImportService {

    private static final String ENTITY = "FinancialTransactionImport";

    private final FinancialTransactionRepository transactionRepository;
    private final AccountingReportGateway gateway;
    private final TenantAuditRecorder auditor;

    public TransactionImportService(FinancialTransactionRepository transactionRepository,
                                    AccountingReportGateway gateway, TenantAuditRecorder auditor) {
        this.transactionRepository = transactionRepository;
        this.gateway = gateway;
        this.auditor = auditor;
    }

    @Transactional
    public CsvImportResult importTransactions(UUID companyId, byte[] fileContent, UUID actor) {
        try {
            TenantContext.setCompanyId(companyId);
            var parsed = new TransactionCsvParser().parse(new String(fileContent, StandardCharsets.UTF_8));
            List<FinancialTransaction> toSave = new ArrayList<>();
            List<CsvLineError> errors = new ArrayList<>(parsed.errors());
            for (CsvImportRow row : parsed.rows()) {
                UUID contactId = null;
                if (row.patientName() != null) {
                    contactId = gateway.resolveContact(companyId, row.patientName());
                    if (contactId == null) {
                        errors.add(new CsvLineError(row.line(),
                                "Paciente não encontrado: " + row.patientName()));
                        continue;
                    }
                }
                UUID professionalId = null;
                if (row.professional() != null) {
                    professionalId = gateway.resolveProfessional(companyId, row.professional());
                    if (professionalId == null) {
                        errors.add(new CsvLineError(row.line(),
                                "Profissional não encontrado: " + row.professional()));
                        continue;
                    }
                }
                toSave.addAll(FinancialTransaction.createSeries(companyId, row.type(), row.description(),
                        row.category(), row.amountCents(), 0L, row.paymentMethod(), row.dueDate(), contactId,
                        professionalId, null, row.notes(), row.paidAt(), 1, actor));
            }
            if (!toSave.isEmpty()) {
                transactionRepository.saveAll(toSave);
            }
            auditor.record(companyId, AuditAction.CREATE, AuditModule.FINANCE, ENTITY, companyId.toString(),
                    "Importação CSV: " + toSave.size() + " lançamentos", actor,
                    Map.of("imported", String.valueOf(toSave.size()), "errors", String.valueOf(errors.size())));
            return new CsvImportResult(toSave.size(), errors);
        } finally {
            TenantContext.clear();
        }
    }
}