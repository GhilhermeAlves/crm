package com.becommerce.crm.clinical.budget.domain;

import com.becommerce.crm.finance.transaction.domain.PaymentMethod;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactConflictException;
import com.becommerce.crm.masterdata.contact.domain.exception.ContactValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orçamento do paciente (Fase 3b). Nasce em {@code DRAFT} (editável), pode ser
 * aprovado uma única vez (com condição de pagamento) e, de {@code DRAFT} ou
 * {@code APPROVED}, marcado como perdido.
 */
public class Budget {

    static final int MAX_TITLE_LENGTH = 160;
    static final int MAX_NOTES_LENGTH = 5000;
    static final int MIN_INSTALLMENTS = 1;
    static final int MAX_INSTALLMENTS = 24;

    private final UUID id;
    private final UUID companyId;
    private final UUID contactId;
    private String title;
    private BudgetStatus status;
    private long discountCents;
    private String notes;
    private PaymentMethod paymentMethod;
    private Integer installments;
    private LocalDate firstDueDate;
    private LocalDateTime approvedAt;
    private UUID approvedBy;
    private LocalDateTime lostAt;
    private List<BudgetItem> items;
    private List<UUID> transactionIds;
    private final UUID createdBy;
    private UUID updatedBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Budget(UUID id, UUID companyId, UUID contactId, String title, BudgetStatus status, long discountCents,
                   String notes, PaymentMethod paymentMethod, Integer installments, LocalDate firstDueDate,
                   LocalDateTime approvedAt, UUID approvedBy, LocalDateTime lostAt, List<BudgetItem> items,
                   List<UUID> transactionIds, UUID createdBy, UUID updatedBy, LocalDateTime createdAt,
                   LocalDateTime updatedAt) {
        this.id = id;
        this.companyId = companyId;
        this.contactId = contactId;
        this.title = title;
        this.status = status;
        this.discountCents = discountCents;
        this.notes = notes;
        this.paymentMethod = paymentMethod;
        this.installments = installments;
        this.firstDueDate = firstDueDate;
        this.approvedAt = approvedAt;
        this.approvedBy = approvedBy;
        this.lostAt = lostAt;
        this.items = items;
        this.transactionIds = transactionIds;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Budget create(UUID companyId, UUID contactId, String title, List<BudgetItem> items,
                                long discountCents, String notes, UUID actorId) {
        LocalDateTime now = LocalDateTime.now();
        List<BudgetItem> validItems = validItems(items);
        return new Budget(UUID.randomUUID(), companyId, contactId, validTitle(title), BudgetStatus.DRAFT,
                validDiscount(discountCents, validItems), normalizeNotes(notes), null, null, null, null, null,
                null, validItems, List.of(), actorId, actorId, now, now);
    }

    public static Budget reconstitute(UUID id, UUID companyId, UUID contactId, String title, BudgetStatus status,
                                      long discountCents, String notes, PaymentMethod paymentMethod,
                                      Integer installments, LocalDate firstDueDate, LocalDateTime approvedAt,
                                      UUID approvedBy, LocalDateTime lostAt, List<BudgetItem> items,
                                      List<UUID> transactionIds, UUID createdBy, UUID updatedBy,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Budget(id, companyId, contactId, title, status, discountCents, notes, paymentMethod,
                installments, firstDueDate, approvedAt, approvedBy, lostAt, List.copyOf(items),
                List.copyOf(transactionIds), createdBy, updatedBy, createdAt, updatedAt);
    }

    public void edit(String newTitle, List<BudgetItem> newItems, long newDiscountCents, String newNotes,
                     UUID actorId) {
        if (!isDraft()) {
            throw new ContactConflictException("Só orçamentos em rascunho podem ser editados.");
        }
        String validTitle = validTitle(newTitle);
        List<BudgetItem> validItems = validItems(newItems);
        this.discountCents = validDiscount(newDiscountCents, validItems);
        this.title = validTitle;
        this.items = validItems;
        this.notes = normalizeNotes(newNotes);
        touch(actorId);
    }

    public void approve(PaymentMethod method, int installmentCount, LocalDate dueDate, UUID actorId) {
        if (!isDraft()) {
            throw new ContactConflictException("Só orçamentos em rascunho podem ser aprovados.");
        }
        if (method == null) {
            throw new ContactValidationException("Informe a forma de pagamento.");
        }
        if (dueDate == null) {
            throw new ContactValidationException("Informe a data do primeiro vencimento.");
        }
        if (installmentCount < MIN_INSTALLMENTS || installmentCount > MAX_INSTALLMENTS) {
            throw new ContactValidationException("O número de parcelas deve estar entre 1 e 24.");
        }
        this.status = BudgetStatus.APPROVED;
        this.paymentMethod = method;
        this.installments = installmentCount;
        this.firstDueDate = dueDate;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = actorId;
        touch(actorId);
    }

    public void linkTransactions(List<UUID> ids) {
        this.transactionIds = List.copyOf(ids);
    }

    public void markLost(UUID actorId) {
        if (isLost()) {
            throw new ContactConflictException("Orçamento já está marcado como perdido.");
        }
        this.status = BudgetStatus.LOST;
        this.lostAt = LocalDateTime.now();
        touch(actorId);
    }

    public void removeItemForProcedure(UUID procedureId, UUID actorId) {
        if (!isDraft()) {
            throw new ContactConflictException("Só orçamentos em rascunho podem ser editados.");
        }
        List<BudgetItem> remaining = new ArrayList<>();
        for (BudgetItem item : items) {
            if (!item.procedureId().equals(procedureId)) {
                remaining.add(new BudgetItem(item.id(), item.procedureId(), item.priceCents(), remaining.size()));
            }
        }
        this.items = List.copyOf(remaining);
        touch(actorId);
    }

    public long subtotalCents() {
        return items.stream().mapToLong(BudgetItem::priceCents).sum();
    }

    public long totalCents() {
        return subtotalCents() - discountCents;
    }

    public boolean isDraft() {
        return status == BudgetStatus.DRAFT;
    }

    public boolean isApproved() {
        return status == BudgetStatus.APPROVED;
    }

    public boolean isLost() {
        return status == BudgetStatus.LOST;
    }

    private void touch(UUID actorId) {
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = actorId;
    }

    private static String validTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new ContactValidationException("O título do orçamento é obrigatório.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new ContactValidationException(
                    "O título do orçamento deve ter no máximo " + MAX_TITLE_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    private static List<BudgetItem> validItems(List<BudgetItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ContactValidationException("Inclua ao menos um procedimento no orçamento.");
        }
        Set<UUID> seen = new HashSet<>();
        List<BudgetItem> result = new ArrayList<>();
        for (BudgetItem item : items) {
            if (!seen.add(item.procedureId())) {
                throw new ContactValidationException("Procedimento informado mais de uma vez no orçamento.");
            }
            result.add(new BudgetItem(item.id(), item.procedureId(), item.priceCents(), result.size()));
        }
        return List.copyOf(result);
    }

    private static long validDiscount(long discountCents, List<BudgetItem> items) {
        if (discountCents < 0) {
            throw new ContactValidationException("O desconto não pode ser negativo.");
        }
        long subtotal = items.stream().mapToLong(BudgetItem::priceCents).sum();
        if (discountCents > subtotal) {
            throw new ContactValidationException("O desconto não pode ser maior que o subtotal.");
        }
        return discountCents;
    }

    private static String normalizeNotes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new ContactValidationException(
                    "As observações devem ter no máximo " + MAX_NOTES_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public UUID getContactId() { return contactId; }
    public String getTitle() { return title; }
    public BudgetStatus getStatus() { return status; }
    public long getDiscountCents() { return discountCents; }
    public String getNotes() { return notes; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public Integer getInstallments() { return installments; }
    public LocalDate getFirstDueDate() { return firstDueDate; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public UUID getApprovedBy() { return approvedBy; }
    public LocalDateTime getLostAt() { return lostAt; }
    public List<BudgetItem> getItems() { return items; }
    public List<UUID> getTransactionIds() { return transactionIds; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
