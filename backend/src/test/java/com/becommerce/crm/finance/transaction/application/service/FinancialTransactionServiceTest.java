package com.becommerce.crm.finance.transaction.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.finance.transaction.application.dto.CreateFinancialTransactionRequest;
import com.becommerce.crm.finance.transaction.application.dto.FinancialTransactionResponse;
import com.becommerce.crm.finance.transaction.application.dto.PayTransactionRequest;
import com.becommerce.crm.finance.transaction.application.dto.UpdateFinancialTransactionRequest;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionQuery;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionNotFoundException;
import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FinancialTransactionServiceTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID OTHER_COMPANY = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();

    private FinancialTransactionRepository repository;
    private FinancialTransactionService service;

    @BeforeEach
    void setUp() {
        repository = mock(FinancialTransactionRepository.class);
        service = new FinancialTransactionService(repository, mock(TenantAuditRecorder.class));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findReferences(any())).thenReturn(Map.of());
    }

    private static CreateFinancialTransactionRequest createRequest(long amountCents, Integer installments) {
        return new CreateFinancialTransactionRequest(TransactionType.INCOME, "Consulta", TransactionCategory.CONSULTATION,
                amountCents, 0L, PaymentMethod.PIX, LocalDate.of(2026, 1, 15), null, null, null, null, null,
                installments, null);
    }

    @Test
    void createSingleTransactionHasNoInstallmentMetadata() {
        List<FinancialTransactionResponse> created = service.create(COMPANY, createRequest(10000L, null), ACTOR);

        assertEquals(1, created.size());
        assertEquals(10000L, created.get(0).amountCents());
        assertNull(created.get(0).installmentNumber());
        assertNull(created.get(0).installmentTotal());
        assertNull(created.get(0).installmentGroupId());
    }

    @Test
    void createSplitsCentsWithRemainderOnFirstInstallmentAndMonthlyDueDates() {
        List<FinancialTransactionResponse> created = service.create(COMPANY, createRequest(10000L, 3), ACTOR);

        assertEquals(3, created.size());
        assertEquals(List.of(3334L, 3333L, 3333L), created.stream()
                .map(FinancialTransactionResponse::amountCents).toList());
        assertEquals(List.of(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 15)),
                created.stream().map(FinancialTransactionResponse::dueDate).toList());
        assertEquals(created.get(0).installmentGroupId(), created.get(2).installmentGroupId());
        assertEquals(List.of(1, 2, 3), created.stream()
                .map(FinancialTransactionResponse::installmentNumber).toList());
    }

    @Test
    void createRejectsCategoryIncompatibleWithType() {
        CreateFinancialTransactionRequest request = new CreateFinancialTransactionRequest(TransactionType.EXPENSE,
                "Aluguel", TransactionCategory.CONSULTATION, 1000L, 0L, PaymentMethod.PIX,
                LocalDate.of(2026, 1, 15), null, null, null, null, null, null, null);

        assertThrows(FinancialTransactionValidationException.class, () -> service.create(COMPANY, request, ACTOR));
    }

    @Test
    void payMarksTransactionAsPaid() {
        FinancialTransaction transaction = FinancialTransaction.create(UUID.randomUUID(), COMPANY,
                TransactionType.INCOME, "Consulta", TransactionCategory.CONSULTATION, 10000L, 0L,
                PaymentMethod.PIX, LocalDate.of(2026, 1, 15), null, null, null, null, null, ACTOR);
        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        FinancialTransactionResponse paid = service.pay(COMPANY, transaction.getId(),
                new PayTransactionRequest(LocalDate.of(2026, 1, 20)), ACTOR);

        assertEquals(TransactionStatus.PAID, paid.status());
        assertEquals(LocalDate.of(2026, 1, 20), paid.paidAt());
    }

    @Test
    void cancelledTransactionCannotBePaid() {
        FinancialTransaction transaction = FinancialTransaction.create(UUID.randomUUID(), COMPANY,
                TransactionType.INCOME, "Consulta", TransactionCategory.CONSULTATION, 10000L, 0L,
                PaymentMethod.PIX, LocalDate.of(2026, 1, 15), null, null, null, null, null, ACTOR);
        transaction.cancel();
        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        assertThrows(FinancialTransactionValidationException.class, () -> service.pay(COMPANY, transaction.getId(),
                new PayTransactionRequest(LocalDate.of(2026, 1, 20)), ACTOR));
    }

    @Test
    void updateOnCancelledTransactionKeepsItCancelled() {
        FinancialTransaction transaction = FinancialTransaction.create(UUID.randomUUID(), COMPANY,
                TransactionType.INCOME, "Consulta", TransactionCategory.CONSULTATION, 10000L, 0L,
                PaymentMethod.PIX, LocalDate.of(2026, 1, 15), null, null, null, null, null, ACTOR);
        transaction.cancel();
        when(repository.findById(transaction.getId())).thenReturn(Optional.of(transaction));

        UpdateFinancialTransactionRequest request = new UpdateFinancialTransactionRequest(TransactionType.EXPENSE,
                "Aluguel", TransactionCategory.RENT, 5000L, 0L, PaymentMethod.TRANSFER, LocalDate.of(2026, 2, 1),
                null, null, null, null, LocalDate.of(2026, 2, 10));
        FinancialTransactionResponse updated = service.update(COMPANY, transaction.getId(), request, ACTOR);

        assertEquals(TransactionStatus.CANCELLED, updated.status());
        assertNull(updated.paidAt());
    }

    @Test
    void transactionOfAnotherCompanyIsNotFound() {
        FinancialTransaction other = FinancialTransaction.create(UUID.randomUUID(), OTHER_COMPANY,
                TransactionType.INCOME, "Consulta", TransactionCategory.CONSULTATION, 10000L, 0L,
                PaymentMethod.PIX, LocalDate.of(2026, 1, 15), null, null, null, null, null, ACTOR);
        when(repository.findById(other.getId())).thenReturn(Optional.of(other));

        assertThrows(FinancialTransactionNotFoundException.class, () -> service.getById(COMPANY, other.getId()));
        assertThrows(FinancialTransactionNotFoundException.class,
                () -> service.cancel(COMPANY, other.getId(), ACTOR));
    }

    @Test
    void overdueIsComputedFromPendingPastDueDate() {
        FinancialTransaction overdue = FinancialTransaction.create(UUID.randomUUID(), COMPANY,
                TransactionType.EXPENSE, "Aluguel", TransactionCategory.RENT, 5000L, 0L,
                PaymentMethod.BOLETO, LocalDate.now().minusDays(1), null, null, null, null, null, ACTOR);
        FinancialTransaction future = FinancialTransaction.create(UUID.randomUUID(), COMPANY,
                TransactionType.EXPENSE, "Aluguel", TransactionCategory.RENT, 5000L, 0L,
                PaymentMethod.BOLETO, LocalDate.now().plusDays(1), null, null, null, null, null, ACTOR);
        when(repository.findById(overdue.getId())).thenReturn(Optional.of(overdue));
        when(repository.findById(future.getId())).thenReturn(Optional.of(future));

        assertTrue(service.getById(COMPANY, overdue.getId()).overdue());
        assertFalse(service.getById(COMPANY, future.getId()).overdue());
    }

    @Test
    void listMapsOverdueStatusToPendingWithDueDateLimitAndClampsPageSize() {
        LocalDate today = LocalDate.now();
        when(repository.search(any(), any(), anyInt(), anyInt()))
                .thenReturn(new FinancialTransactionRepository.PageResult(List.of(), 0));

        service.list(COMPANY, null, "overdue", null, null, null, null, null, null, "  ", -3, 5000);

        ArgumentCaptor<FinancialTransactionQuery> captor = ArgumentCaptor.forClass(FinancialTransactionQuery.class);
        verify(repository).search(any(), captor.capture(), anyInt(), anyInt());
        FinancialTransactionQuery filter = captor.getValue();
        assertEquals(TransactionStatus.PENDING.name(), filter.status());
        assertEquals(today, filter.overdueBefore());
        assertNull(filter.pattern());

        verify(repository).search(any(), any(), eq(0), eq(100));
    }
}
