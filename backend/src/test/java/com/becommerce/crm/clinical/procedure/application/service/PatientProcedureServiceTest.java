package com.becommerce.crm.clinical.procedure.application.service;

import com.becommerce.crm.analytics.audit.application.service.TenantAuditRecorder;
import com.becommerce.crm.clinical.procedure.application.dto.CompleteProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureRequest;
import com.becommerce.crm.clinical.procedure.application.dto.PatientProcedureResponse;
import com.becommerce.crm.clinical.procedure.application.dto.ProcedureToothDto;
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
import com.becommerce.crm.masterdata.catalog.application.port.output.CatalogItemRepository;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItem;
import com.becommerce.crm.masterdata.catalog.domain.CatalogItemType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientProcedureServiceTest {

    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final UUID CONTACT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID ITEM_ID = UUID.randomUUID();

    @Mock
    private PatientProcedureRepository repository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private CatalogItemRepository catalogRepository;

    @Mock
    private FinancialTransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantAuditRecorder auditor;

    @Mock
    private CatalogItem catalogItem;

    private PatientProcedureService service;
    private FinancialTransaction pendingTx;

    @BeforeEach
    void setUp() {
        service = new PatientProcedureService(repository, contactRepository, catalogRepository,
                transactionRepository, userRepository, auditor);
        Contact contact = mock(Contact.class);
        lenient().when(contact.getCompanyId()).thenReturn(COMPANY_ID);
        lenient().when(contactRepository.findById(CONTACT_ID)).thenReturn(Optional.of(contact));
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(catalogItem.getCompanyId()).thenReturn(COMPANY_ID);
        lenient().when(catalogItem.getType()).thenReturn(CatalogItemType.SERVICE);
        lenient().when(catalogItem.isActive()).thenReturn(true);
        lenient().when(catalogItem.getName()).thenReturn("Restauração");
        lenient().when(catalogItem.getPrice()).thenReturn(new BigDecimal("150.00"));
        lenient().when(catalogItem.getCategory()).thenReturn("Dentística");
        lenient().when(catalogRepository.findById(ITEM_ID)).thenReturn(Optional.of(catalogItem));
    }

    private PatientProcedureRequest request(ProcedureState state, long price, LocalDate performedAt,
                                            boolean generate) {
        return new PatientProcedureRequest(ITEM_ID, state, Dentition.PERMANENT, price, null, null, performedAt,
                List.of(new ProcedureToothDto(11, Set.of(ToothFace.M))), generate, null);
    }

    private PatientProcedure existing(ProcedureState state) {
        PatientProcedure p = PatientProcedure.create(COMPANY_ID, CONTACT_ID, ITEM_ID, "Restauração", state,
                Dentition.PERMANENT, 15000, null, null,
                state == ProcedureState.DONE ? LocalDate.of(2026, 10, 1) : null,
                List.of(new ProcedureTooth(11, Set.of(ToothFace.M))), ACTOR_ID);
        lenient().when(repository.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private PatientProcedure existingWithTransaction(TransactionStatus status) {
        PatientProcedure p = existing(ProcedureState.DONE);
        pendingTx = FinancialTransaction.create(UUID.randomUUID(), COMPANY_ID, TransactionType.INCOME, "x",
                TransactionCategory.PROCEDURE, 15000, 0, PaymentMethod.OTHER, LocalDate.now(), CONTACT_ID, null,
                ITEM_ID, null, status == TransactionStatus.PAID ? LocalDate.now() : null, null);
        p.linkTransaction(pendingTx.getId());
        when(transactionRepository.findById(pendingTx.getId())).thenReturn(Optional.of(pendingTx));
        return p;
    }

    @Test
    void createsPlannedWithCatalogSnapshot() {
        PatientProcedureResponse r = service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.PLANNED, 15000, null, false), ACTOR_ID, false);
        assertThat(r.name()).isEqualTo("Restauração");
        assertThat(r.category()).isEqualTo("Dentística");
        assertThat(r.transaction()).isNull();
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void rejectsProductOrInactiveCatalogItem() {
        when(catalogItem.getType()).thenReturn(CatalogItemType.PRODUCT);
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.PLANNED, 0, null, false), ACTOR_ID, false))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void doneWithGenerateCreatesPendingIncome() {
        PatientProcedureResponse r = service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.DONE, 15000, LocalDate.of(2026, 10, 22), true), ACTOR_ID, true);
        ArgumentCaptor<FinancialTransaction> captor = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(captor.capture());
        FinancialTransaction tx = captor.getValue();
        assertThat(tx.getType()).isEqualTo(TransactionType.INCOME);
        assertThat(tx.getCategory()).isEqualTo(TransactionCategory.PROCEDURE);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(tx.getAmountCents()).isEqualTo(15000);
        assertThat(tx.getContactId()).isEqualTo(CONTACT_ID);
        assertThat(tx.getDescription()).isEqualTo("Procedimento: Restauração – dentes 11");
        assertThat(tx.getDueDate()).isEqualTo(LocalDate.now());
        assertThat(r.transaction().status()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    void generateWithoutFinancePermissionIsForbidden() {
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.DONE, 15000, LocalDate.now(), true), ACTOR_ID, false))
                .isInstanceOf(CrmAccessDeniedException.class);
    }

    @Test
    void generateWithZeroPriceIsRejected() {
        assertThatThrownBy(() -> service.create(COMPANY_ID, CONTACT_ID,
                request(ProcedureState.DONE, 0, LocalDate.now(), true), ACTOR_ID, true))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void generateIgnoredWhenNotDone() {
        service.create(COMPANY_ID, CONTACT_ID, request(ProcedureState.PLANNED, 15000, null, true), ACTOR_ID, true);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void completeGeneratesTransaction() {
        PatientProcedure p = existing(ProcedureState.PLANNED);
        PatientProcedureResponse r = service.complete(COMPANY_ID, CONTACT_ID, p.getId(),
                new CompleteProcedureRequest(LocalDate.of(2026, 10, 22), null, true, LocalDate.of(2026, 11, 5)),
                ACTOR_ID, true);
        assertThat(r.state()).isEqualTo(ProcedureState.DONE);
        assertThat(r.transaction()).isNotNull();
    }

    @Test
    void updatingDoneOnlyAcceptsNotes() {
        PatientProcedure p = existing(ProcedureState.DONE);
        PatientProcedureRequest sameButNotes = new PatientProcedureRequest(ITEM_ID, ProcedureState.DONE,
                Dentition.PERMANENT, p.getPriceCents(), null, "ok", p.getPerformedAt(),
                List.of(new ProcedureToothDto(11, Set.of(ToothFace.M))), false, null);
        assertThat(service.update(COMPANY_ID, CONTACT_ID, p.getId(), sameButNotes, ACTOR_ID).notes())
                .isEqualTo("ok");
        PatientProcedureRequest changedPrice = new PatientProcedureRequest(ITEM_ID, ProcedureState.DONE,
                Dentition.PERMANENT, 99L, null, "ok", p.getPerformedAt(),
                List.of(new ProcedureToothDto(11, Set.of(ToothFace.M))), false, null);
        assertThatThrownBy(() -> service.update(COMPANY_ID, CONTACT_ID, p.getId(), changedPrice, ACTOR_ID))
                .isInstanceOf(ContactConflictException.class);
    }

    @Test
    void deleteWithPaidTransactionIsConflict() {
        PatientProcedure p = existingWithTransaction(TransactionStatus.PAID);
        assertThatThrownBy(() -> service.delete(COMPANY_ID, CONTACT_ID, p.getId(), ACTOR_ID))
                .isInstanceOf(ContactConflictException.class)
                .hasMessageContaining("pagamento registrado");
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteWithPendingTransactionCancelsIt() {
        PatientProcedure p = existingWithTransaction(TransactionStatus.PENDING);
        service.delete(COMPANY_ID, CONTACT_ID, p.getId(), ACTOR_ID);
        assertThat(pendingTx.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        verify(transactionRepository).save(pendingTx);
        verify(repository).delete(p);
    }

    @Test
    void listFiltersByStateCategoryAndTooth() {
        PatientProcedure a = existing(ProcedureState.PLANNED);
        PatientProcedure b = PatientProcedure.create(COMPANY_ID, CONTACT_ID, ITEM_ID, "Limpeza",
                ProcedureState.PRE_EXISTING, Dentition.PERMANENT, 0, null, null, null, List.of(), null);
        when(repository.findByCompanyIdAndContactIdOrderByCreatedAtDesc(COMPANY_ID, CONTACT_ID))
                .thenReturn(List.of(a, b));
        assertThat(service.list(COMPANY_ID, CONTACT_ID, ProcedureState.PLANNED, null, null)).hasSize(1);
        assertThat(service.list(COMPANY_ID, CONTACT_ID, null, null, 11)).hasSize(1);
        assertThat(service.list(COMPANY_ID, CONTACT_ID, null, "Dentística", null)).hasSize(2);
        assertThat(service.list(COMPANY_ID, CONTACT_ID, null, "Outra", null)).isEmpty();
    }
}
