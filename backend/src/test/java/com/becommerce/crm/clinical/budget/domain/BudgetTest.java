package com.becommerce.crm.clinical.budget.domain;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetTest {

    private static final UUID COMPANY = UUID.randomUUID();
    private static final UUID CONTACT = UUID.randomUUID();
    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROC_A = UUID.randomUUID();
    private static final UUID PROC_B = UUID.randomUUID();

    private static BudgetItem item(UUID procedureId, long price) {
        return new BudgetItem(UUID.randomUUID(), procedureId, price, 99);
    }

    private static Budget draft() {
        return Budget.create(COMPANY, CONTACT, "Plano", List.of(item(PROC_A, 15000), item(PROC_B, 10000)), 5000,
                null, ACTOR);
    }

    @Test
    void createComputesTotalsAndPositions() {
        Budget budget = draft();

        assertThat(budget.getStatus()).isEqualTo(BudgetStatus.DRAFT);
        assertThat(budget.isDraft()).isTrue();
        assertThat(budget.subtotalCents()).isEqualTo(25000);
        assertThat(budget.totalCents()).isEqualTo(20000);
        assertThat(budget.getItems()).extracting(BudgetItem::position).containsExactly(0, 1);
    }

    @Test
    void createValidatesInput() {
        List<BudgetItem> ok = List.of(item(PROC_A, 15000));
        assertThatThrownBy(() -> Budget.create(COMPANY, CONTACT, "  ", ok, 0, null, ACTOR))
                .isInstanceOf(ContactValidationException.class);
        assertThatThrownBy(() -> Budget.create(COMPANY, CONTACT, "Plano", List.of(), 0, null, ACTOR))
                .isInstanceOf(ContactValidationException.class);
        assertThatThrownBy(() -> Budget.create(COMPANY, CONTACT, "Plano",
                List.of(item(PROC_A, 100), item(PROC_A, 200)), 0, null, ACTOR))
                .isInstanceOf(ContactValidationException.class);
        assertThatThrownBy(() -> Budget.create(COMPANY, CONTACT, "Plano", ok, 15001, null, ACTOR))
                .isInstanceOf(ContactValidationException.class)
                .hasMessageContaining("desconto");
        assertThatThrownBy(() -> Budget.create(COMPANY, CONTACT, "Plano", ok, -1, null, ACTOR))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void approveMovesToApprovedOnce() {
        Budget budget = draft();
        budget.approve(PaymentMethod.PIX, 3, LocalDate.of(2026, 11, 1), ACTOR);

        assertThat(budget.isApproved()).isTrue();
        assertThat(budget.getApprovedAt()).isNotNull();
        assertThat(budget.getApprovedBy()).isEqualTo(ACTOR);
        assertThat(budget.getInstallments()).isEqualTo(3);
        assertThatThrownBy(() -> budget.approve(PaymentMethod.PIX, 3, LocalDate.of(2026, 11, 1), ACTOR))
                .isInstanceOf(ContactConflictException.class);
    }

    @Test
    void approveRejectsInvalidInstallments() {
        LocalDate due = LocalDate.of(2026, 11, 1);
        assertThatThrownBy(() -> draft().approve(PaymentMethod.PIX, 0, due, ACTOR))
                .isInstanceOf(ContactValidationException.class);
        assertThatThrownBy(() -> draft().approve(PaymentMethod.PIX, 25, due, ACTOR))
                .isInstanceOf(ContactValidationException.class);
        assertThatThrownBy(() -> draft().approve(null, 1, due, ACTOR))
                .isInstanceOf(ContactValidationException.class);
        assertThatThrownBy(() -> draft().approve(PaymentMethod.PIX, 1, null, ACTOR))
                .isInstanceOf(ContactValidationException.class);
    }

    @Test
    void editOnlyInDraft() {
        Budget budget = draft();
        budget.edit("Novo", List.of(item(PROC_A, 1000)), 0, " obs ", ACTOR);
        assertThat(budget.getTitle()).isEqualTo("Novo");
        assertThat(budget.getNotes()).isEqualTo("obs");
        budget.approve(PaymentMethod.CASH, 1, LocalDate.of(2026, 11, 1), ACTOR);

        assertThatThrownBy(() -> budget.edit("X", List.of(item(PROC_A, 1000)), 0, null, ACTOR))
                .isInstanceOf(ContactConflictException.class)
                .hasMessage("Só orçamentos em rascunho podem ser editados.");
    }

    @Test
    void markLostFromDraftAndApprovedButNotTwice() {
        Budget fromDraft = draft();
        fromDraft.markLost(ACTOR);
        assertThat(fromDraft.isLost()).isTrue();
        assertThat(fromDraft.getLostAt()).isNotNull();
        assertThatThrownBy(() -> fromDraft.markLost(ACTOR)).isInstanceOf(ContactConflictException.class);

        Budget fromApproved = draft();
        fromApproved.approve(PaymentMethod.PIX, 2, LocalDate.of(2026, 11, 1), ACTOR);
        fromApproved.markLost(ACTOR);
        assertThat(fromApproved.isLost()).isTrue();
    }

    @Test
    void removeItemReindexesOnlyInDraft() {
        Budget budget = draft();
        budget.removeItemForProcedure(PROC_A, ACTOR);

        assertThat(budget.getItems()).hasSize(1);
        assertThat(budget.getItems().get(0).procedureId()).isEqualTo(PROC_B);
        assertThat(budget.getItems().get(0).position()).isZero();

        Budget approved = draft();
        approved.approve(PaymentMethod.PIX, 1, LocalDate.of(2026, 11, 1), ACTOR);
        assertThatThrownBy(() -> approved.removeItemForProcedure(PROC_A, ACTOR))
                .isInstanceOf(ContactConflictException.class);
    }
}
