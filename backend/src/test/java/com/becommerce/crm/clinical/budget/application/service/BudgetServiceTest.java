package com.becommerce.crm.clinical.budget.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.clinical.budget.application.dto.ApproveBudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetRequest;
import com.becommerce.crm.clinical.budget.application.dto.BudgetResponse;
import com.becommerce.crm.clinical.budget.application.port.out.BudgetRepository;
import com.becommerce.crm.clinical.budget.domain.Budget;
import com.becommerce.crm.clinical.budget.domain.BudgetItem;
import com.becommerce.crm.clinical.budget.domain.BudgetStatus;
import com.becommerce.crm.clinical.procedure.application.port.out.PatientProcedureRepository;
import com.becommerce.crm.clinical.procedure.domain.Dentition;
import com.becommerce.crm.clinical.procedure.domain.PatientProcedure;
import com.becommerce.crm.clinical.procedure.domain.ProcedureState;
import com.becommerce.crm.clinical.procedure.domain.ProcedureTooth;
import com.becommerce.crm.clinical.procedure.domain.ToothFace;
import com.becommerce.crm.finance.transaction.application.port.output.FinancialTransactionRepository;
import com.becommerce.crm.finance.transaction.domain.FinancialTransaction;
import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.finance.transaction.domain.TransactionCategory;
import com.becommerce.crm.finance.transaction.domain.TransactionStatus;
import com.becommerce.crm.finance.transaction.domain.TransactionType;
import com.becommerce.crm.identity.application.port.output.UserRepository;
import com.becommerce.crm.identity.domain.exception.CrmAccessDeniedException;
import com.becommerce.crm.masterdata.contact.application.port.out.ContactRepository;
import com.becommerce.crm.masterdata.contact.domain.Contact;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID OTHER_CONTACT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    @Mock
    private BudgetRepository repository;

    @Mock
    private PatientProcedureRepository procedureRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantAuditRecorder auditor;

    private BudgetService service;

    @BeforeEach
    void setUp() {
        service = new BudgetService(repository, procedureRepository, contactRepository, transactionRepository,
                userRepository, auditor);
        Contact contact = mock(Contact.class);
        lenient().when(contact.getCompanyId()).thenReturn(COMPANY_ID);
        lenient().when(contactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private PatientProcedure procedure(UUID contactId, ProcedureState state, long price) {
        PatientProcedure p = PatientProcedure.create(COMPANY_ID, contactId, ITEM_ID, "Restauração", state,
                Dentition.PERMANENT, price, null, null,
                state == ProcedureState.DONE ? LocalDate.of(2026, 10, 1) : null,
                List.of(new ProcedureTooth(11, Set.of(ToothFace.M))), ACTOR_ID);
        lenient().when(procedureRepository.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private BudgetRequest request(long discount, Map<UUID, Long> prices, PatientProcedure... procedures) {
        return new BudgetRequest("Tratamento", java.util.Arrays.stream(procedures).map(PatientProcedure::getId).toList(),
                discount, null, prices);
    }

    private Budget draft(PatientProcedure... procedures) {
        List<BudgetItem> items = new java.util.ArrayList<>();
        for (PatientProcedure p : procedures) {
            items.add(new BudgetItem(UUID.randomUUID(), p.getId(), p.getPriceCents(), items.size()));
        }
        Budget budget = Budget.create(COMPANY_ID, CONTACT_ID, "Tratamento", items, 0L, null, ACTOR_ID);
        lenient().when(repository.findById(budget.getId())).thenReturn(Optional.of(budget));
        return budget;
    }

    private ApproveBudgetRequest approval(int installments) {
        return new ApproveBudgetRequest(PaymentMethod.PIX, installments, LocalDate.of(2026, 11, 10));
    }

    @Test
    void createsWithSubtotalAndTotal() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.PLANNED, 10000);
        PatientProcedure b = procedure(CONTACT_ID, ProcedureState.PLANNED, 5000);
        BudgetResponse r = service.create(COMPANY_ID, CONTACT_ID, request(2000, null, a, b), ACTOR_ID);
        assertThat(r.status()).isEqualTo(BudgetStatus.DRAFT);
        assertThat(r.subtotalCents()).isEqualTo(15000);
        assertThat(r.discountCents()).isEqualTo(2000);
        assertThat(r.totalCents()).isEqualTo(13000);
        assertThat(r.items()).hasSize(2);
        assertThat(r.items().get(0).name()).isEqualTo("Restauração");
    }

    @Test
    void rejectsProcedureFromAnotherPatient() {
        PatientProcedure a = procedure(OTHER_CONTACT_ID, ProcedureState.PLANNED, 10000);
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID, request(0, null, a), ACTOR_ID))
                .isInstanceOf(ContactValidationException.class)
                .hasMessage("Procedimento não encontrado para este paciente.");
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsDoneProcedure() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.DONE, 10000);
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID, request(0, null, a), ACTOR_ID))
                .isInstanceOf(ContactValidationException.class)
                .hasMessage("Só procedimentos \"A realizar\" podem entrar no orçamento.");
    }

    @Test
    void rejectsProcedureInAnotherActiveBudget() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.PLANNED, 10000);
        Budget other = draft(a);
        when(repository.findActiveByProcedureIds(any(), anyCollection())).thenReturn(Map.of(a.getId(), other));
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID, request(0, null, a), ACTOR_ID))
                .isInstanceOf(ContactConflictException.class)
                .hasMessage("O procedimento Restauração já está no orçamento Tratamento.");
    }

    @Test
    void updateKeepsSavedPriceAndItemIdAndAcceptsItemPrices() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.PLANNED, 10000);
        PatientProcedure b = procedure(CONTACT_ID, ProcedureState.PLANNED, 5000);
        Budget budget = Budget.create(COMPANY_ID, CONTACT_ID, "Tratamento",
                List.of(new BudgetItem(UUID.randomUUID(), a.getId(), 9000, 0)), 0L, null, ACTOR_ID);
        UUID originalItemId = budget.getItems().get(0).id();
        when(repository.findById(budget.getId())).thenReturn(Optional.of(budget));
        when(repository.findActiveByProcedureIds(any(), anyCollection())).thenReturn(Map.of(a.getId(), budget));

        BudgetResponse r = service.update(COMPANY_ID, CONTACT_ID, budget.getId(),
                request(0, Map.of(b.getId(), 4000L), a, b), ACTOR_ID);

        assertThat(r.items().get(0).priceCents()).isEqualTo(9000);
        assertThat(r.items().get(1).priceCents()).isEqualTo(4000);
        assertThat(r.subtotalCents()).isEqualTo(13000);
        assertThat(budget.getItems().get(0).id()).isEqualTo(originalItemId);
        assertThat(budget.getItems().get(1).id()).isNotEqualTo(originalItemId);
        verify(repository).save(budget);
    }

    @Test
    void updateWithItemPriceOverridesSavedOne() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.PLANNED, 10000);
        Budget budget = Budget.create(COMPANY_ID, CONTACT_ID, "Tratamento",
                List.of(new BudgetItem(UUID.randomUUID(), a.getId(), 9000, 0)), 0L, null, ACTOR_ID);
        when(repository.findById(budget.getId())).thenReturn(Optional.of(budget));

        BudgetResponse r = service.update(COMPANY_ID, CONTACT_ID, budget.getId(),
                request(0, Map.of(a.getId(), 7000L), a), ACTOR_ID);
        assertThat(r.items().get(0).priceCents()).isEqualTo(7000);
    }

    @Test
    void approveCreatesThreeInstallmentsWithRemainderOnFirst() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.PLANNED, 10000);
        Budget budget = draft(a);
        when(transactionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse r = service.approve(COMPANY_ID, CONTACT_ID, budget.getId(), approval(3), ACTOR_ID, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FinancialTransaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionRepository).saveAll(captor.capture());
        List<FinancialTransaction> saved = captor.getValue();
        assertThat(saved).extracting(FinancialTransaction::getAmountCents).containsExactly(3334L, 3333L, 3333L);
        assertThat(saved).allSatisfy(tx -> {
            assertThat(tx.getCategory()).isEqualTo(TransactionCategory.PROCEDURE);
            assertThat(tx.getType()).isEqualTo(TransactionType.INCOME);
            assertThat(tx.getDescription()).startsWith("Orçamento: Tratamento");
        });
        assertThat(r.status()).isEqualTo(BudgetStatus.APPROVED);
        assertThat(budget.getTransactionIds()).hasSize(3);
    }

    @Test
    void approveWithoutPermissionIsForbidden() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.PLANNED, 10000);
        Budget budget = draft(a);
        assertThatThrownBy(() -> service.approve(COMPANY_ID, CONTACT_ID, budget.getId(), approval(1), ACTOR_ID, false))
                .isInstanceOf(CrmAccessDeniedException.class);
        verify(transactionRepository, never()).saveAll(anyList());
        verify(repository, never()).save(any());
        assertThat(budget.isDraft()).isTrue();
    }

    @Test
    void approveWithZeroTotalGeneratesNoInstallments() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.PLANNED, 0);
        Budget budget = draft(a);

        BudgetResponse r = service.approve(COMPANY_ID, CONTACT_ID, budget.getId(), approval(1), ACTOR_ID, true);

        verify(transactionRepository, never()).saveAll(anyList());
        assertThat(r.status()).isEqualTo(BudgetStatus.APPROVED);
        assertThat(r.installmentsList()).isEmpty();
    }

    private Budget approvedWith(FinancialTransaction... txs) {
        Budget budget = Budget.reconstitute(UUID.randomUUID(), COMPANY_ID, CONTACT_ID, "Tratamento",
                BudgetStatus.APPROVED, 0L, null, PaymentMethod.PIX, txs.length, LocalDate.of(2026, 11, 10),
                LocalDateTime.now(), ACTOR_ID, null,
                List.of(new BudgetItem(UUID.randomUUID(), UUID.randomUUID(), 10000, 0)),
                java.util.Arrays.stream(txs).map(FinancialTransaction::getId).toList(), ACTOR_ID, ACTOR_ID,
                LocalDateTime.now(), LocalDateTime.now());
        lenient().when(repository.findById(budget.getId())).thenReturn(Optional.of(budget));
        for (FinancialTransaction tx : txs) {
            lenient().when(transactionRepository.findById(tx.getId())).thenReturn(Optional.of(tx));
        }
        return budget;
    }

    private FinancialTransaction tx(boolean paid) {
        return FinancialTransaction.create(UUID.randomUUID(), COMPANY_ID, TransactionType.INCOME, "x",
                TransactionCategory.PROCEDURE, 5000, 0, PaymentMethod.PIX, LocalDate.of(2026, 11, 10), CONTACT_ID,
                null, null, null, paid ? LocalDate.now() : null, ACTOR_ID);
    }

    @Test
    void loseWithPaidInstallmentIsConflict() {
        FinancialTransaction paid = tx(true);
        FinancialTransaction pending = tx(false);
        Budget budget = approvedWith(paid, pending);
        assertThatThrownBy(() -> service.lose(COMPANY_ID, CONTACT_ID, budget.getId(), ACTOR_ID))
                .isInstanceOf(ContactConflictException.class)
                .hasMessage("Orçamento com parcela paga não pode ser marcado como perdido.");
        assertThat(pending.getStatus()).isEqualTo(TransactionStatus.PENDING);
        verify(repository, never()).save(any());
    }

    @Test
    void loseCancelsPendingInstallments() {
        FinancialTransaction first = tx(false);
        FinancialTransaction second = tx(false);
        Budget budget = approvedWith(first, second);

        BudgetResponse r = service.lose(COMPANY_ID, CONTACT_ID, budget.getId(), ACTOR_ID);

        assertThat(r.status()).isEqualTo(BudgetStatus.LOST);
        assertThat(first.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        assertThat(second.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        verify(transactionRepository).save(first);
        verify(transactionRepository).save(second);
    }

    @Test
    void deleteApprovedIsConflict() {
        Budget budget = approvedWith();
        assertThatThrownBy(() -> service.delete(COMPANY_ID, CONTACT_ID, budget.getId(), ACTOR_ID))
                .isInstanceOf(ContactConflictException.class)
                .hasMessage("Orçamento aprovado não pode ser excluído. Marque como perdido antes.");
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteDraftRemoves() {
        PatientProcedure a = procedure(CONTACT_ID, ProcedureState.PLANNED, 10000);
        Budget budget = draft(a);
        service.delete(COMPANY_ID, CONTACT_ID, budget.getId(), ACTOR_ID);
        verify(repository).delete(budget);
    }
}
