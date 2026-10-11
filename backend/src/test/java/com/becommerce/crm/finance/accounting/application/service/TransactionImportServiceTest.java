package com.becommerce.crm.finance.accounting.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.analytics.audit.domain.AuditAction;
import com.becommerce.crm.analytics.audit.domain.AuditModule;
import com.becommerce.crm.finance.accounting.application.dto.CsvImportResult;
import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportGateway;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionImportServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID ANA_ID = UUID.randomUUID();
    private static final UUID DRA_ID = UUID.randomUUID();
    private static final String HEADER =
            "Tipo;Descrição;Categoria;Valor;Forma de pagamento;Vencimento;Pago em;Paciente;Profissional;"
                    + "Observações";

    private FinancialTransactionRepository repository;
    private AccountingReportGateway gateway;
    private TenantAuditRecorder auditor;
    private TransactionImportService service;

    @BeforeEach
    void setUp() {
        repository = mock(FinancialTransactionRepository.class);
        gateway = mock(AccountingReportGateway.class);
        auditor = mock(TenantAuditRecorder.class);
        service = new TransactionImportService(repository, gateway, auditor);
    }

    @Test
    void importsValidRowsAndCollectsResolutionErrors() {
        String csv = HEADER + "\n"
                + "Receita;Consulta de retorno;Procedimento;200,00;Pix;05/01/2026;05/01/2026;"
                + "Ana Souza;Dra. Maria;\n"
                + "Receita;Paciente desconhecido;Consulta;150,00;Pix;05/01/2026;;Ninguem;;\n"
                + "Despesa;Aluguel;Aluguel;5.000,00;Boleto;10/01/2026;;;;\n";
        when(gateway.resolveContact(COMPANY, "Ana Souza")).thenReturn(ANA_ID);
        when(gateway.resolveProfessional(COMPANY, "Dra. Maria")).thenReturn(DRA_ID);
        when(gateway.resolveContact(COMPANY, "Ninguem")).thenReturn(null);

        CsvImportResult result = service.importTransactions(COMPANY, csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), ACTOR);

        assertEquals(2, result.imported());
        assertEquals(1, result.errors().size());
        assertEquals(3, result.errors().get(0).line());
        assertEquals("Paciente não encontrado: Ninguem", result.errors().get(0).message());
        ArgumentCaptor<List<FinancialTransaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(com.becommerce.crm.finance.transaction.domain.TransactionStatus.PAID,
                captor.getValue().get(0).getStatus());
        assertEquals(com.becommerce.crm.finance.transaction.domain.TransactionStatus.PENDING,
                captor.getValue().get(1).getStatus());
    }

    @Test
    void recordsSingleAuditEventWithImportedCount() {
        String csv = HEADER + "\n"
                + "Despesa;Aluguel;Aluguel;5.000,00;Boleto;10/01/2026;;;;\n";

        service.importTransactions(COMPANY, csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), ACTOR);

        verify(auditor).record(eq(COMPANY), eq(AuditAction.CREATE), eq(AuditModule.FINANCE),
                eq("FinancialTransactionImport"), eq(COMPANY.toString()),
                eq("Importação CSV: 1 lançamentos"), eq(ACTOR), any());
    }

    @Test
    void noValidRowsSkipsPersistenceAndAuditsZero() {
        String csv = HEADER + "\n"
                + "Receita;Paciente desconhecido;Consulta;150,00;Pix;05/01/2026;;Ninguem;;\n";
        when(gateway.resolveContact(COMPANY, "Ninguem")).thenReturn(null);

        CsvImportResult result = service.importTransactions(COMPANY, csv.getBytes(java.nio.charset.StandardCharsets.UTF_8), ACTOR);

        assertEquals(0, result.imported());
        assertEquals(1, result.errors().size());
        verify(repository, never()).saveAll(any());
        verify(auditor).record(eq(COMPANY), eq(AuditAction.CREATE), eq(AuditModule.FINANCE),
                eq("FinancialTransactionImport"), eq(COMPANY.toString()),
                eq("Importação CSV: 0 lançamentos"), eq(ACTOR), any());
    }
}