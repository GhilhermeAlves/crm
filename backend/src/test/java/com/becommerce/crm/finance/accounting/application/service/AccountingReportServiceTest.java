package com.becommerce.crm.finance.accounting.application.service;

import com.becommerce.crm.finance.accounting.application.dto.AccountingReportRequestDto;
import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportGateway;
import com.becommerce.crm.finance.accounting.application.port.output.AccountingReportRequestRepository;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountingReportServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final YearMonth MONTH = YearMonth.of(2026, 1);

    private AccountingReportGateway gateway;
    private AccountingReportRequestRepository register;
    private AccountingReportService service;

    @BeforeEach
    void setUp() {
        gateway = mock(AccountingReportGateway.class);
        register = mock(AccountingReportRequestRepository.class);
        service = new AccountingReportService(gateway, register, new AccountingReportRenderer());
    }

    @Test
    void monthlyAccountingCsvHasSectionsAndTotals() {
        when(gateway.companyName(any())).thenReturn("Clínica Teste");
        when(register.register(any(), any(), any(), any(), any())).thenReturn(1);
        when(gateway.monthlyAccounting(any(), any(), any())).thenReturn(List.of(
                new AccountingReportGateway.CategoryTotalRow(
                        com.becommerce.crm.finance.transaction.domain.TransactionType.INCOME,
                        com.becommerce.crm.finance.transaction.domain.TransactionCategory.PROCEDURE,
                        5000L, 500L),
                new AccountingReportGateway.CategoryTotalRow(
                        com.becommerce.crm.finance.transaction.domain.TransactionType.INCOME,
                        com.becommerce.crm.finance.transaction.domain.TransactionCategory.CONSULTATION,
                        1500L, 0L),
                new AccountingReportGateway.CategoryTotalRow(
                        com.becommerce.crm.finance.transaction.domain.TransactionType.EXPENSE,
                        com.becommerce.crm.finance.transaction.domain.TransactionCategory.RENT,
                        2000L, 0L)));

        byte[] bytes = service.generate(COMPANY, request("MONTHLY_ACCOUNTING", "CSV"), UUID.randomUUID());
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("Tipo;Categoria;Valor"));
        assertTrue(csv.contains("Receita;Procedimento;50,00"));
        assertTrue(csv.contains("Receita;Consulta;15,00"));
        assertTrue(csv.contains("Despesa;Aluguel;20,00"));
        assertTrue(csv.contains("Total de receitas;65,00"));
        assertTrue(csv.contains("Total de despesas;20,00"));
        assertTrue(csv.contains("Taxas;5,00"));
        assertTrue(csv.contains("Resultado;45,00"));
    }

    @Test
    void statementCsvKeepsRunningBalance() {
        when(gateway.companyName(any())).thenReturn("Clínica Teste");
        when(register.register(any(), any(), any(), any(), any())).thenReturn(1);
        when(gateway.statement(any(), any(), any())).thenReturn(List.of(
                new AccountingReportGateway.StatementRow(LocalDate.of(2026, 1, 10), "Consulta",
                        com.becommerce.crm.finance.transaction.domain.TransactionCategory.CONSULTATION,
                        com.becommerce.crm.finance.transaction.domain.TransactionType.INCOME, 15000L),
                new AccountingReportGateway.StatementRow(LocalDate.of(2026, 1, 5), "Aluguel",
                        com.becommerce.crm.finance.transaction.domain.TransactionCategory.RENT,
                        com.becommerce.crm.finance.transaction.domain.TransactionType.EXPENSE, 20000L)));

        String csv = new String(service.generate(COMPANY, request("STATEMENT", "CSV"),
                UUID.randomUUID()), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Data;Descrição;Categoria;Entrada;Saída;Saldo"));
        assertTrue(csv.contains("10/01/2026;Consulta;Consulta;150,00;;150,00"));
        assertTrue(csv.contains("05/01/2026;Aluguel;Aluguel;;200,00;-50,00"));
    }

    @Test
    void transactionsCsvIncludesParcelaStatusAndNet() {
        when(gateway.companyName(any())).thenReturn("Clínica Teste");
        when(register.register(any(), any(), any(), any(), any())).thenReturn(1);
        when(gateway.transactionDetails(any(), any(), any())).thenReturn(List.of(
                new AccountingReportGateway.TransactionDetailRow(LocalDate.of(2026, 1, 5),
                        LocalDate.of(2026, 1, 6), "Consulta de retorno",
                        com.becommerce.crm.finance.transaction.domain.TransactionCategory.CONSULTATION,
                        com.becommerce.crm.finance.transaction.domain.TransactionType.INCOME, 20000L, 500L,
                        com.becommerce.crm.finance.transaction.domain.PaymentMethod.PIX,
                        TransactionStatus.PAID, 2, 3, "Ana Souza", "Dra. Maria")));

        String csv = new String(service.generate(COMPANY, request("TRANSACTIONS", "CSV"),
                UUID.randomUUID()), StandardCharsets.UTF_8);

        assertTrue(csv.contains("Vencimento;Pagamento;Descrição;Categoria;Status;Valor;Taxa;Líquido;"
                + "Forma de pagamento;Parcela;Paciente;Profissional"));
        assertTrue(csv.contains("05/01/2026;06/01/2026;Consulta de retorno;Consulta;Pago;200,00;5,00;"
                + "195,00;Pix;2/3;Ana Souza;Dra. Maria"));
    }

    @Test
    void pdfStartsWithPdfMagic() {
        when(gateway.companyName(any())).thenReturn("Clínica Teste");
        when(register.register(any(), any(), any(), any(), any())).thenReturn(1);
        when(gateway.monthlyAccounting(any(), any(), any())).thenReturn(List.of());

        byte[] pdf = service.generate(COMPANY, request("MONTHLY_ACCOUNTING", "PDF"), UUID.randomUUID());

        assertTrue(pdf.length > 800);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.ISO_8859_1));
    }

    @Test
    void validatesTypeFormatAndMonth() {
        assertThrows(FinancialTransactionValidationException.class,
                () -> service.generate(COMPANY, request("NOT_A_TYPE", "CSV"), UUID.randomUUID()));
        assertThrows(FinancialTransactionValidationException.class,
                () -> service.generate(COMPANY, request("MONTHLY_ACCOUNTING", "XLS"),
                        UUID.randomUUID()));
        assertThrows(FinancialTransactionValidationException.class,
                () -> service.generate(COMPANY, new AccountingReportRequestDto("MONTHLY_ACCOUNTING",
                        "2026-13", "CSV"), UUID.randomUUID()));
        assertThrows(FinancialTransactionValidationException.class,
                () -> service.generate(COMPANY, new AccountingReportRequestDto("", "2026-01", "CSV"),
                        UUID.randomUUID()));
    }

    @Test
    void generatesReportFileName() {
        assertEquals("relatorio-monthly-accounting-2026-01.csv",
                service.fileName(request("MONTHLY_ACCOUNTING", "CSV")));
        assertEquals("relatorio-statement-2026-09.pdf",
                service.fileName(new AccountingReportRequestDto("STATEMENT", "2026-09", "PDF")));
        assertEquals("relatorio-transactions-2025-12.pdf",
                service.fileName(new AccountingReportRequestDto("TRANSACTIONS", "2025-12", "PDF")));
    }

    @Test
    void availableMonthsPassesThroughGateway() {
        when(gateway.availableMonths(any())).thenReturn(List.of("2026-02", "2026-01"));

        List<String> months = service.availableMonths(COMPANY);

        assertEquals(List.of("2026-02", "2026-01"), months);
        verify(gateway).availableMonths(COMPANY);
    }

    private static AccountingReportRequestDto request(String type, String format) {
        return new AccountingReportRequestDto(type, MONTH.toString(), format);
    }
}