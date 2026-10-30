package com.becommerce.crm.finance.transaction.domain;

import com.becommerce.crm.finance.transaction.domain.exception.FinancialTransactionValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lançamento financeiro de uma empresa (raiz do agregado, tabela
 * {@code financial_transactions}). Valores em centavos ({@code long}).
 *
 * <p>Regras: a categoria precisa ser compatível com o tipo (receita x despesa);
 * {@code paidAt} só pode existir com status {@link TransactionStatus#PAID};
 * pagar exige a data e não é permitido em lançamento {@code CANCELLED}; cancelar
 * não apaga o registro (apenas marca o status).
 *
 * <p>"Atrasado" não é status gravado: é {@code PENDING} com {@code dueDate}
 * anterior a hoje ({@link #isOverdue(java.time.LocalDate)}).
 */
public class FinancialTransaction {

    private static final int DESCRIPTION_MAX = 255;
    private static final int INSTALLMENTS_MAX = 24;

    private final UUID id;
    private final UUID companyId;
    private TransactionType type;
    private String description;
    private TransactionCategory category;
    private long amountCents;
    private long feeCents;
    private PaymentMethod paymentMethod;
    private LocalDate dueDate;
    private LocalDate paidAt;
    private TransactionStatus status;
    private UUID contactId;
    private UUID professionalUserId;
    private UUID catalogItemId;
    private final UUID installmentGroupId;
    private final Integer installmentNumber;
    private final Integer installmentTotal;
    private String notes;
    private final UUID createdBy;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private FinancialTransaction(UUID id, UUID companyId, TransactionType type, String description,
                                 TransactionCategory category, long amountCents, long feeCents,
                                 PaymentMethod paymentMethod, LocalDate dueDate, LocalDate paidAt,
                                 TransactionStatus status, UUID contactId, UUID professionalUserId,
                                 UUID catalogItemId, UUID installmentGroupId, Integer installmentNumber,
                                 Integer installmentTotal, String notes, UUID createdBy,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.companyId = Objects.requireNonNull(companyId, "companyId");
        applyCore(type, description, category, amountCents, feeCents, paymentMethod, dueDate);
        this.contactId = contactId;
        this.professionalUserId = professionalUserId;
        this.catalogItemId = catalogItemId;
        this.installmentGroupId = installmentGroupId;
        this.installmentNumber = installmentNumber;
        this.installmentTotal = installmentTotal;
        this.notes = normalizeNotes(notes);
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.paidAt = paidAt;
        this.status = status == null ? TransactionStatus.PENDING : status;
        validatePaymentConsistency();
    }

    /** Cria um lançamento único (sem parcelamento). */
    public static FinancialTransaction create(UUID id, UUID companyId, TransactionType type, String description,
                                              TransactionCategory category, long amountCents, long feeCents,
                                              PaymentMethod paymentMethod, LocalDate dueDate, UUID contactId,
                                              UUID professionalUserId, UUID catalogItemId, String notes,
                                              LocalDate paidAt, UUID createdBy) {
        return create(id, companyId, type, description, category, amountCents, feeCents, paymentMethod, dueDate,
                contactId, professionalUserId, catalogItemId, notes, paidAt, createdBy, null, null, null);
    }

    private static FinancialTransaction create(UUID id, UUID companyId, TransactionType type, String description,
                                               TransactionCategory category, long amountCents, long feeCents,
                                               PaymentMethod paymentMethod, LocalDate dueDate, UUID contactId,
                                               UUID professionalUserId, UUID catalogItemId, String notes,
                                               LocalDate paidAt, UUID createdBy, UUID installmentGroupId,
                                               Integer installmentNumber, Integer installmentTotal) {
        LocalDateTime now = LocalDateTime.now();
        TransactionStatus status = paidAt == null ? TransactionStatus.PENDING : TransactionStatus.PAID;
        return new FinancialTransaction(id, companyId, type, description, category, amountCents, feeCents,
                paymentMethod, dueDate, paidAt, status, contactId, professionalUserId, catalogItemId,
                installmentGroupId, installmentNumber, installmentTotal, notes, createdBy, now, now);
    }

    /**
     * Gera 1..{@value #INSTALLMENTS_MAX} lançamentos irmãos. O valor é dividido
     * igualmente e o resto dos centavos vai na 1ª parcela (ex.: 10000 em 3 =
     * 3334, 3333, 3333). Os vencimentos são mensais a partir de {@code dueDate}.
     */
    public static List<FinancialTransaction> createSeries(UUID companyId, TransactionType type, String description,
                                                          TransactionCategory category, long amountCents,
                                                          long feeCents, PaymentMethod paymentMethod,
                                                          LocalDate dueDate, UUID contactId,
                                                          UUID professionalUserId, UUID catalogItemId, String notes,
                                                          LocalDate paidAt, int installments, UUID createdBy) {
        if (installments < 1 || installments > INSTALLMENTS_MAX) {
            throw new FinancialTransactionValidationException(
                    "O número de parcelas deve estar entre 1 e " + INSTALLMENTS_MAX + ".");
        }
        if (amountCents <= 0) {
            throw new FinancialTransactionValidationException("O valor do lançamento deve ser maior que zero.");
        }
        if (amountCents < installments) {
            throw new FinancialTransactionValidationException(
                    "O valor é pequeno demais para ser dividido em " + installments + " parcelas.");
        }
        UUID groupId = installments > 1 ? UUID.randomUUID() : null;
        long base = amountCents / installments;
        long remainder = amountCents % installments;
        List<FinancialTransaction> result = new ArrayList<>();
        for (int number = 1; number <= installments; number++) {
            long value = base + (number == 1 ? remainder : 0L);
            Integer numberOrNull = installments > 1 ? number : null;
            Integer totalOrNull = installments > 1 ? installments : null;
            result.add(create(UUID.randomUUID(), companyId, type, description, category, value, feeCents,
                    paymentMethod, dueDate.plusMonths(number - 1L), contactId, professionalUserId, catalogItemId,
                    notes, paidAt, createdBy, groupId, numberOrNull, totalOrNull));
        }
        return result;
    }

    /**
     * Lançamento recorrente (aluguel, salário...): {@code months} lançamentos irmãos
     * com o valor cheio, vencimentos mensais a partir de {@code dueDate}. Só o 1º
     * pode nascer pago; os seguintes ficam pendentes.
     */
    public static List<FinancialTransaction> createRecurring(UUID companyId, TransactionType type,
                                                             String description, TransactionCategory category,
                                                             long amountCents, long feeCents,
                                                             PaymentMethod paymentMethod, LocalDate dueDate,
                                                             UUID contactId, UUID professionalUserId,
                                                             UUID catalogItemId, String notes, LocalDate paidAt,
                                                             int months, UUID createdBy) {
        if (months < 2 || months > INSTALLMENTS_MAX) {
            throw new FinancialTransactionValidationException(
                    "A recorrência deve ter entre 2 e " + INSTALLMENTS_MAX + " meses.");
        }
        if (amountCents <= 0) {
            throw new FinancialTransactionValidationException("O valor do lançamento deve ser maior que zero.");
        }
        UUID groupId = UUID.randomUUID();
        List<FinancialTransaction> result = new ArrayList<>();
        for (int number = 1; number <= months; number++) {
            result.add(create(UUID.randomUUID(), companyId, type, description, category, amountCents, feeCents,
                    paymentMethod, dueDate.plusMonths(number - 1L), contactId, professionalUserId, catalogItemId,
                    notes, number == 1 ? paidAt : null, createdBy, groupId, number, months));
        }
        return result;
    }

    public static FinancialTransaction reconstitute(UUID id, UUID companyId, TransactionType type,
                                                    String description, TransactionCategory category,
                                                    long amountCents, long feeCents, PaymentMethod paymentMethod,
                                                    LocalDate dueDate, LocalDate paidAt, TransactionStatus status,
                                                    UUID contactId, UUID professionalUserId, UUID catalogItemId,
                                                    UUID installmentGroupId, Integer installmentNumber,
                                                    Integer installmentTotal, String notes, UUID createdBy,
                                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new FinancialTransaction(id, companyId, type, description, category, amountCents, feeCents,
                paymentMethod, dueDate, paidAt, status, contactId, professionalUserId, catalogItemId,
                installmentGroupId, installmentNumber, installmentTotal, notes, createdBy, createdAt, updatedAt);
    }

    /** Edita os dados do lançamento. Um lançamento cancelado permanece cancelado. */
    public void update(TransactionType type, String description, TransactionCategory category, long amountCents,
                       long feeCents, PaymentMethod paymentMethod, LocalDate dueDate, UUID contactId,
                       UUID professionalUserId, UUID catalogItemId, String notes, LocalDate paidAt) {
        applyCore(type, description, category, amountCents, feeCents, paymentMethod, dueDate);
        this.contactId = contactId;
        this.professionalUserId = professionalUserId;
        this.catalogItemId = catalogItemId;
        this.notes = normalizeNotes(notes);
        if (this.status != TransactionStatus.CANCELLED) {
            this.paidAt = paidAt;
            this.status = paidAt == null ? TransactionStatus.PENDING : TransactionStatus.PAID;
        }
        validatePaymentConsistency();
        touch();
    }

    /** Marca como pago ({@link TransactionStatus#PAID}). Não se paga um lançamento cancelado. */
    public void pay(LocalDate paidAt) {
        if (paidAt == null) {
            throw new FinancialTransactionValidationException("A data de pagamento é obrigatória.");
        }
        if (this.status == TransactionStatus.CANCELLED) {
            throw new FinancialTransactionValidationException("Não é possível pagar um lançamento cancelado.");
        }
        this.paidAt = paidAt;
        this.status = TransactionStatus.PAID;
        touch();
    }

    /** Cancela o lançamento sem apagá-lo (zera {@code paidAt}). */
    public void cancel() {
        this.status = TransactionStatus.CANCELLED;
        this.paidAt = null;
        touch();
    }

    /** "Atrasado" = pendente com vencimento anterior a hoje (não é status gravado). */
    public boolean isOverdue(LocalDate today) {
        return this.status == TransactionStatus.PENDING && this.dueDate.isBefore(today);
    }

    private void applyCore(TransactionType type, String description, TransactionCategory category,
                           long amountCents, long feeCents, PaymentMethod paymentMethod, LocalDate dueDate) {
        if (type == null) {
            throw new FinancialTransactionValidationException("O tipo do lançamento é obrigatório.");
        }
        if (description == null || description.isBlank()) {
            throw new FinancialTransactionValidationException("A descrição do lançamento é obrigatória.");
        }
        String trimmed = description.trim();
        if (trimmed.length() > DESCRIPTION_MAX) {
            throw new FinancialTransactionValidationException(
                    "A descrição do lançamento deve ter no máximo " + DESCRIPTION_MAX + " caracteres.");
        }
        if (category == null) {
            throw new FinancialTransactionValidationException("A categoria do lançamento é obrigatória.");
        }
        if (category.type() != type) {
            throw new FinancialTransactionValidationException(
                    "A categoria " + category + " não é compatível com o tipo " + type + ".");
        }
        if (amountCents <= 0) {
            throw new FinancialTransactionValidationException("O valor do lançamento deve ser maior que zero.");
        }
        if (feeCents < 0) {
            throw new FinancialTransactionValidationException("A taxa não pode ser negativa.");
        }
        if (paymentMethod == null) {
            throw new FinancialTransactionValidationException("A forma de pagamento é obrigatória.");
        }
        if (dueDate == null) {
            throw new FinancialTransactionValidationException("O vencimento é obrigatório.");
        }
        this.type = type;
        this.description = trimmed;
        this.category = category;
        this.amountCents = amountCents;
        this.feeCents = feeCents;
        this.paymentMethod = paymentMethod;
        this.dueDate = dueDate;
    }

    private void validatePaymentConsistency() {
        if (this.paidAt != null && this.status != TransactionStatus.PAID) {
            throw new FinancialTransactionValidationException(
                    "A data de pagamento só pode existir com o status PAID.");
        }
        if (this.status == TransactionStatus.PAID && this.paidAt == null) {
            throw new FinancialTransactionValidationException("Um lançamento pago precisa da data de pagamento.");
        }
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private static String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }

    public UUID getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public TransactionType getType() { return type; }
    public String getDescription() { return description; }
    public TransactionCategory getCategory() { return category; }
    public long getAmountCents() { return amountCents; }
    public long getFeeCents() { return feeCents; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getPaidAt() { return paidAt; }
    public TransactionStatus getStatus() { return status; }
    public UUID getContactId() { return contactId; }
    public UUID getProfessionalUserId() { return professionalUserId; }
    public UUID getCatalogItemId() { return catalogItemId; }
    public UUID getInstallmentGroupId() { return installmentGroupId; }
    public Integer getInstallmentNumber() { return installmentNumber; }
    public Integer getInstallmentTotal() { return installmentTotal; }
    public String getNotes() { return notes; }
    public UUID getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
